/**
 * 文件：app/src/main/java/kittoku/osc/SharedBridge.kt
 * 作用说明：
 * 本文件定义了 SharedBridge 类，作为 SSTP VPN 服务的核心桥接器，负责管理协议协商、参数同步、终端实例、消息传递等。
 * 主要功能：
 * - 统一管理 VPN 服务相关的参数（如认证、MTU/MRU、IP 地址、允许的应用等），并提供线程安全的帧 ID 分配。
 * - 持有 SSL/IP 终端实例，负责协议数据的收发和处理。
 * - 提供控制消息通道，用于各模块间的异步通信。
 * - 通过偏好设置接口，获取和校验用户配置参数。
 * 在本项目中，SstpVpnService、SSLTerminal、IPTerminal、协议协商与链路管理等模块会调用本文件，
 * 用于访问和操作 VPN 连接的全局状态、参数和消息。
 * 本文件会调用 kittoku.osc.preference、kittoku.osc.service.SstpVpnService、kittoku.osc.terminal 相关模块，
 * 并被 VPN 服务主流程及各协议终端依赖和调用。
 */

package kittoku.osc

import androidx.preference.PreferenceManager
import kittoku.osc.preference.AppString
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.getValidAllowedAppInfos
import kittoku.osc.service.SstpVpnService
import kittoku.osc.terminal.IPTerminal
import kittoku.osc.terminal.SSLTerminal
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// VPN 协议各阶段标识枚举
internal enum class Where {
    CERT, CERT_PATH, SSL, PROXY, SSTP_DATA, SSTP_CONTROL, SSTP_REQUEST, SSTP_HASH,
    PPP, PAP, CHAP, MSCAHPV2, EAP, LCP, LCP_MRU, LCP_AUTH, IPCP, IPCP_IP,
    IPV6CP, IPV6CP_IDENTIFIER, IP, IPv4, IPv6, ROUTE, INCOMING, OUTGOING,
}

// 控制消息数据结构，包含来源、结果及补充信息
internal data class ControlMessage(
    val from: Where,
    val result: Result,
    val supplement: String? = null
)

// 控制消息结果枚举，涵盖各协议常见状态与错误
internal enum class Result {
    PROCEEDED,
    // 通用错误
    ERR_TIMEOUT, ERR_COUNT_EXHAUSTED, ERR_UNKNOWN_TYPE, ERR_UNEXPECTED_MESSAGE,
    ERR_PARSING_FAILED, ERR_VERIFICATION_FAILED,
    // SSTP 专用
    ERR_NEGATIVE_ACKNOWLEDGED, ERR_ABORT_REQUESTED, ERR_DISCONNECT_REQUESTED,
    // PPP 专用
    ERR_TERMINATE_REQUESTED, ERR_PROTOCOL_REJECTED, ERR_CODE_REJECTED,
    ERR_AUTHENTICATION_FAILED, ERR_ADDRESS_REJECTED, ERR_OPTION_REJECTED,
    // IP 专用
    ERR_INVALID_ADDRESS,
    // INCOMING 专用
    ERR_INVALID_PACKET_SIZE,
}

/**
 * SharedBridge 类
 * 负责管理 VPN 服务的全局状态、参数、消息通道及协议终端实例
 */
internal class SharedBridge(internal val service: SstpVpnService) {
    // 偏好设置对象
    internal val prefs = PreferenceManager.getDefaultSharedPreferences(service)
    // VPN Builder 实例
    internal val builder = service.Builder()
    // 协程异常处理器
    internal lateinit var handler: CoroutineExceptionHandler

    // 控制消息通道，用于异步通信
    internal val controlMailbox = Channel<ControlMessage>(Channel.BUFFERED)

    // SSL/IP 协议终端实例
    internal var sslTerminal: SSLTerminal? = null
    internal var ipTerminal: IPTerminal? = null

    // 用户认证与 PPP 参数
    internal val HOME_USERNAME = getStringPrefValue(OscPrefKey.HOME_USERNAME, prefs)
    internal val HOME_PASSWORD = getStringPrefValue(OscPrefKey.HOME_PASSWORD, prefs)
    internal val PPP_MRU = getIntPrefValue(OscPrefKey.PPP_MRU, prefs)
    internal val PPP_MTU = getIntPrefValue(OscPrefKey.PPP_MTU, prefs)
    internal val PPP_AUTH_PROTOCOLS = getSetPrefValue(OscPrefKey.PPP_AUTH_PROTOCOLS, prefs)
    internal val PPP_IPv4_ENABLED = getBooleanPrefValue(OscPrefKey.PPP_IPv4_ENABLED, prefs)
    internal val PPP_IPv6_ENABLED = getBooleanPrefValue(OscPrefKey.PPP_IPv6_ENABLED, prefs)

    // SSTP 协议相关参数
    internal var hlak: ByteArray? = null
    internal val nonce = ByteArray(32)
    internal val guid = UUID.randomUUID().toString()
    internal var hashProtocol: Byte = 0

    // 帧 ID 分配互斥锁及计数器
    private val mutex = Mutex()
    private var frameID = -1

    // 当前协商参数
    internal var currentMRU = PPP_MRU
    internal var currentAuth = ""
    internal val currentIPv4 = ByteArray(4)
    internal val currentIPv6 = ByteArray(8)
    internal val currentProposedDNS = ByteArray(4)

    // 允许访问的应用列表
    internal val allowedApps: List<AppString> = mutableListOf<AppString>().also {
        if (getBooleanPrefValue(OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)) {
            getValidAllowedAppInfos(prefs, service.packageManager).forEach { info ->
                it.add(
                    AppString(
                        info.packageName,
                        service.packageManager.getApplicationLabel(info).toString()
                    )
                )
            }
        }
    }

    /**
     * 判断指定认证协议是否启用
     * @param authProtocol 协议名称
     * @return 是否启用
     */
    internal fun isEnabled(authProtocol: String): Boolean {
        return authProtocol in PPP_AUTH_PROTOCOLS
    }

    /**
     * 绑定 SSL 协议终端
     */
    internal fun attachSSLTerminal() {
        sslTerminal = SSLTerminal(this)
    }

    /**
     * 绑定 IP 协议终端
     */
    internal fun attachIPTerminal() {
        ipTerminal = IPTerminal(this)
    }

    /**
     * 分配新的帧 ID，线程安全
     * @return 新的帧 ID
     */
    internal suspend fun allocateNewFrameID(): Byte {
        mutex.withLock {
            frameID += 1
            return frameID.toByte()
        }
    }
}