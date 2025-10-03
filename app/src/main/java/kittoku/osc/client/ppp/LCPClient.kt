/**
 * 文件：LCPClient.kt
 * 作用说明：
 * 本文件实现了 PPP 协议 LCP（Link Control Protocol）协商阶段的客户端 LCPClient，用于处理链路参数（如 MRU、认证方式等）的协商请求、应答、拒绝等流程。
 * 在本软件项目中，PPP 链路建立过程中会调用该文件，主要由 PPP 主流程控制模块（如 PPPManager 或相关连接管理类）实例化和调用 LCPClient，以完成 LCP 协商。
 * 本文件会调用 ConfigClient（作为父类）、SharedBridge（通信桥）、ControlMessage（控制消息）、LCPConfigureFrame 及相关 LCP 选项类，负责与底层 PPP 协议和配置选项交互。
 * 作为 PPP 协议 LCP 阶段的具体实现，简化了链路参数协商的流程管理，提高了代码复用性和模块化。
 */

package kittoku.osc.client.ppp

import kittoku.osc.ControlMessage
import kittoku.osc.DEFAULT_MRU
import kittoku.osc.MIN_MRU
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.preference.AUTH_PROTOCOL_EAP_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOL_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOl_PAP
import kittoku.osc.unit.ppp.LCPConfigureAck
import kittoku.osc.unit.ppp.LCPConfigureFrame
import kittoku.osc.unit.ppp.LCPConfigureNak
import kittoku.osc.unit.ppp.LCPConfigureReject
import kittoku.osc.unit.ppp.LCPConfigureRequest
import kittoku.osc.unit.ppp.PPP_PROTOCOL_CHAP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_EAP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_PAP
import kittoku.osc.unit.ppp.option.AuthOption
import kittoku.osc.unit.ppp.option.CHAP_ALGORITHM_MSCHAPv2
import kittoku.osc.unit.ppp.option.LCPOptionPack
import kittoku.osc.unit.ppp.option.MRUOption
import kotlin.math.max
import kotlin.math.min

// LCP 协商客户端，继承自 ConfigClient，负责处理链路参数（如 MRU、认证方式）的协商
internal class LCPClient(bridge: SharedBridge) : ConfigClient<LCPConfigureFrame>(Where.LCP, bridge) {
    // 标记 MRU 是否被拒绝
    private var isMruRejected = false

    /**
     * 服务端尝试生成 REJECT 响应，拒绝未知选项
     * @param request 收到的协商请求帧
     * @return 若有需拒绝的选项则返回 REJECT 帧，否则返回 null
     */
    override fun tryCreateServerReject(request: LCPConfigureFrame): LCPConfigureFrame? {
        val reject = LCPOptionPack()

        if (request.options.unknownOptions.isNotEmpty()) {
            reject.unknownOptions = request.options.unknownOptions
        }

        return if (reject.allOptions.isNotEmpty()) {
            LCPConfigureReject().also {
                it.id = request.id
                it.options = reject
                it.options.order = request.options.order
            }
        } else null
    }

    /**
     * 服务端尝试生成 NAK 响应，协商 MRU 和认证方式
     * @param request 收到的协商请求帧
     * @return 若有需协商的选项则返回 NAK 帧，否则返回 null
     */
    override fun tryCreateServerNak(request: LCPConfigureFrame): LCPConfigureFrame? {
        val nak = LCPOptionPack()

        // 协商 MRU
        val serverMru = request.options.mruOption?.unitSize ?: DEFAULT_MRU
        if (serverMru < bridge.PPP_MTU) {
            nak.mruOption = MRUOption().also { it.unitSize = bridge.PPP_MTU }
        }

        // 协商认证方式
        val serverAuth = request.options.authOption
        var isAuthAcknowledged = false

        when (serverAuth?.protocol) {
            PPP_PROTOCOL_EAP -> {
                if (bridge.isEnabled(AUTH_PROTOCOL_EAP_MSCHAPv2)) {
                    bridge.currentAuth = AUTH_PROTOCOL_EAP_MSCHAPv2
                    isAuthAcknowledged = true
                }
            }
            PPP_PROTOCOL_CHAP -> {
                if (serverAuth.holder.size != 1) {
                    throw NotImplementedError(serverAuth.holder.size.toString())
                }
                if (serverAuth.holder[0] == CHAP_ALGORITHM_MSCHAPv2 && bridge.isEnabled(AUTH_PROTOCOL_MSCHAPv2)) {
                    bridge.currentAuth = AUTH_PROTOCOL_MSCHAPv2
                    isAuthAcknowledged = true
                }
            }
            PPP_PROTOCOL_PAP -> {
                if (bridge.isEnabled(AUTH_PROTOCOl_PAP)) {
                    bridge.currentAuth = AUTH_PROTOCOl_PAP
                    isAuthAcknowledged = true
                }
            }
        }

        // 若认证方式未被接受，则生成 NAK 认证选项
        if (!isAuthAcknowledged) {
            val authOption = AuthOption()
            when {
                bridge.isEnabled(AUTH_PROTOCOL_EAP_MSCHAPv2) -> {
                    authOption.protocol = PPP_PROTOCOL_EAP
                }
                bridge.isEnabled(AUTH_PROTOCOL_MSCHAPv2) -> {
                    authOption.protocol = PPP_PROTOCOL_CHAP
                    authOption.holder = ByteArray(1) { CHAP_ALGORITHM_MSCHAPv2 }
                }
                bridge.isEnabled(AUTH_PROTOCOl_PAP) -> {
                    authOption.protocol = PPP_PROTOCOL_PAP
                }
                else -> throw NotImplementedError()
            }
            nak.authOption = authOption
        }

        return if (nak.allOptions.isNotEmpty()) {
            LCPConfigureNak().also {
                it.id = request.id
                it.options = nak
                it.options.order = request.options.order
            }
        } else null
    }

    /**
     * 服务端生成 ACK 响应，直接确认客户端请求的选项
     * @param request 收到的协商请求帧
     * @return ACK 响应帧
     */
    override fun createServerAck(request: LCPConfigureFrame): LCPConfigureFrame {
        return LCPConfigureAck().also {
            it.id = request.id
            it.options = request.options
        }
    }

    /**
     * 客户端生成请求帧，包含 MRU 选项（若未被拒绝）
     * @return 协商请求帧
     */
    override fun createClientRequest(): LCPConfigureFrame {
        val request = LCPConfigureRequest()

        if (!isMruRejected) {
            request.options.mruOption = MRUOption().also { it.unitSize = bridge.currentMRU }
        }

        return request
    }

    /**
     * 客户端处理收到的 NAK 响应，更新 MRU
     * @param nak 收到的 NAK 响应帧
     */
    override suspend fun tryAcceptClientNak(nak: LCPConfigureFrame) {
        nak.options.mruOption?.also {
            bridge.currentMRU = max(min(it.unitSize, bridge.PPP_MRU), MIN_MRU)
        }
    }

    /**
     * 客户端处理收到的 REJECT 响应，MRU 或认证方式被拒绝则发送错误消息
     * @param reject 收到的 REJECT 响应帧
     */
    override suspend fun tryAcceptClientReject(reject: LCPConfigureFrame) {
        reject.options.mruOption?.also {
            isMruRejected = true
            if (DEFAULT_MRU > bridge.PPP_MRU) {
                bridge.controlMailbox.send(
                    ControlMessage(Where.LCP_MRU, Result.ERR_OPTION_REJECTED)
                )
            }
        }
        reject.options.authOption?.also {
            bridge.controlMailbox.send(ControlMessage(Where.LCP_AUTH, Result.ERR_OPTION_REJECTED))
        }
    }
}