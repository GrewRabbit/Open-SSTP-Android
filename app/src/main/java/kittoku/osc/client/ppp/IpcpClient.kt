/**
 * 文件：IpcpClient.kt
 * 作用说明：
 * 本文件实现了 PPP 协议 IPCP（IP Control Protocol）协商阶段的客户端 `IpcpClient`，用于处理 IPv4 地址和 DNS 的协商请求、应答、拒绝等流程。
 * 在本软件项目中，PPP 协议链路建立过程中会调用该文件，主要由 PPP 主流程控制模块（如 PPPManager 或相关连接管理类）实例化和调用 `IpcpClient`，以完成 IPCP 协商。
 * 本文件会调用 `ConfigClient`（作为父类）、`SharedBridge`（通信桥）、`ControlMessage`（控制消息）、`IpcpConfigureFrame` 及相关 IPCP 选项类，负责与底层 PPP 协议和配置选项交互。
 * 作为 PPP 协议 IPCP 阶段的具体实现，简化了 IPv4 地址和 DNS 协商的流程管理，提高了代码复用性和模块化。
 */

package kittoku.osc.client.ppp

import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.unit.ppp.IpcpConfigureAck
import kittoku.osc.unit.ppp.IpcpConfigureFrame
import kittoku.osc.unit.ppp.IpcpConfigureReject
import kittoku.osc.unit.ppp.IpcpConfigureRequest
import kittoku.osc.unit.ppp.option.IpcpAddressOption
import kittoku.osc.unit.ppp.option.IpcpOptionPack
import kittoku.osc.unit.ppp.option.OPTION_TYPE_IPCP_DNS
import kittoku.osc.unit.ppp.option.OPTION_TYPE_IPCP_IP
import java.net.Inet4Address

// IPCP 协商客户端，继承自 ConfigClient，负责处理 IPv4 地址和 DNS 的协商
internal class IpcpClient(bridge: SharedBridge) : ConfigClient<IpcpConfigureFrame>(Where.IPCP, bridge) {
    // 是否请求 DNS 地址
    private val isDNSRequested = getBooleanPrefValue(OscPrefKey.DNS_DO_REQUEST_ADDRESS, bridge.prefs)
    // DNS 是否被拒绝
    private var isDNSRejected = false
    // 请求的静态 IPv4 地址（如果配置了静态地址）
    private val requestedAddress = if (getBooleanPrefValue(OscPrefKey.PPP_DO_REQUEST_STATIC_IPv4_ADDRESS, bridge.prefs)) {
        Inet4Address.getByName(getStringPrefValue(OscPrefKey.PPP_STATIC_IPv4_ADDRESS, bridge.prefs)).address
    } else {
        null
    }

    /**
     * 服务端尝试生成 REJECT 响应，拒绝未知选项和 DNS 选项
     */
    override fun tryCreateServerReject(request: IpcpConfigureFrame): IpcpConfigureFrame? {
        val reject = IpcpOptionPack()

        // 拒绝未知选项
        if (request.options.unknownOptions.isNotEmpty()) {
            reject.unknownOptions = request.options.unknownOptions
        }

        // 拒绝 DNS 选项（客户端不提供 DNS 服务器）
        request.options.dnsOption?.also {
            reject.dnsOption = request.options.dnsOption
        }

        // 如果有需要拒绝的选项则返回 REJECT 帧，否则返回 null
        return if (reject.allOptions.isNotEmpty()) {
            IpcpConfigureReject().also {
                it.id = request.id
                it.options = reject
                it.options.order = request.options.order
            }
        } else null
    }

    /**
     * 服务端尝试生成 NAK 响应（本实现不处理 NAK，直接返回 null）
     */
    override fun tryCreateServerNak(request: IpcpConfigureFrame): IpcpConfigureFrame? {
        return null
    }

    /**
     * 服务端生成 ACK 响应，直接确认客户端请求的选项
     */
    override fun createServerAck(request: IpcpConfigureFrame): IpcpConfigureFrame {
        return IpcpConfigureAck().also {
            it.id = request.id
            it.options = request.options
        }
    }

    /**
     * 客户端生成请求帧，包含 IPv4 地址和 DNS 选项（根据配置和协商结果决定是否包含 DNS）
     */
    override fun createClientRequest(): IpcpConfigureFrame {
        val request = IpcpConfigureRequest()

        // 如果有静态地址则写入当前 IPv4
        requestedAddress?.also {
            it.copyInto(bridge.currentIPv4)
        }

        // 添加 IPv4 地址选项
        request.options.ipOption = IpcpAddressOption(OPTION_TYPE_IPCP_IP).also {
            bridge.currentIPv4.copyInto(it.address)
        }

        // 根据配置和协商结果决定是否添加 DNS 选项
        if (isDNSRequested && !isDNSRejected) {
            request.options.dnsOption = IpcpAddressOption(OPTION_TYPE_IPCP_DNS).also {
                bridge.currentProposedDNS.copyInto(it.address)
            }
        }

        return request
    }

    /**
     * 客户端处理收到的 NAK 响应，更新 IPv4 或 DNS 地址，或发送错误消息
     */
    override suspend fun tryAcceptClientNak(nak: IpcpConfigureFrame) {
        nak.options.ipOption?.also {
            if (requestedAddress != null) {
                bridge.controlMailbox.send(ControlMessage(Where.IPCP, Result.ERR_ADDRESS_REJECTED))
            } else {
                it.address.copyInto(bridge.currentIPv4)
            }
        }

        nak.options.dnsOption?.also {
            it.address.copyInto(bridge.currentProposedDNS)
        }
    }

    /**
     * 客户端处理收到的 REJECT 响应，DNS 被拒绝则不再请求，IP 被拒绝则发送错误消息
     */
    override suspend fun tryAcceptClientReject(reject: IpcpConfigureFrame) {
        reject.options.ipOption?.also {
            bridge.controlMailbox.send(ControlMessage(Where.IPCP_IP, Result.ERR_OPTION_REJECTED))
        }

        reject.options.dnsOption?.also {
            isDNSRejected = true
        }
    }
}
