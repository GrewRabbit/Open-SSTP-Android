/**
 * 文件：Ipv6cpClient.kt
 * 作用说明：
 * 本文件实现了 PPP 协议 IPv6CP（IPv6 Control Protocol）协商阶段的客户端 Ipv6cpClient，用于处理 IPv6 标识符的协商请求、应答、拒绝等流程。
 * 在本软件项目中，PPP 协议链路建立过程中会调用该文件，主要由 PPP 主流程控制模块（如 PPPManager 或相关连接管理类）实例化和调用 Ipv6cpClient，以完成 IPv6CP 协商。
 * 本文件会调用 ConfigClient（作为父类）、SharedBridge（通信桥）、ControlMessage（控制消息）、Ipv6cpConfigureFrame 及相关 IPv6CP 选项类，负责与底层 PPP 协议和配置选项交互。
 * 作为 PPP 协议 IPv6CP 阶段的具体实现，简化了 IPv6 标识符协商的流程管理，提高了代码复用性和模块化。
 */

package kittoku.osc.client.ppp

import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.unit.ppp.Ipv6cpConfigureAck
import kittoku.osc.unit.ppp.Ipv6cpConfigureFrame
import kittoku.osc.unit.ppp.Ipv6cpConfigureReject
import kittoku.osc.unit.ppp.Ipv6cpConfigureRequest
import kittoku.osc.unit.ppp.option.Ipv6cpIdentifierOption
import kittoku.osc.unit.ppp.option.Ipv6cpOptionPack

// IPv6CP 协商客户端，继承自 ConfigClient，负责处理 IPv6 标识符的协商
internal class Ipv6cpClient(bridge: SharedBridge) : ConfigClient<Ipv6cpConfigureFrame>(Where.IPV6CP, bridge) {
    /**
     * 服务端尝试生成 REJECT 响应，拒绝未知选项
     * @param request 收到的协商请求帧
     * @return 若有需拒绝的选项则返回 REJECT 帧，否则返回 null
     */
    override fun tryCreateServerReject(request: Ipv6cpConfigureFrame): Ipv6cpConfigureFrame? {
        val reject = Ipv6cpOptionPack()

        if (request.options.unknownOptions.isNotEmpty()) {
            reject.unknownOptions = request.options.unknownOptions
        }

        return if (reject.allOptions.isNotEmpty()) {
            Ipv6cpConfigureReject().also {
                it.id = request.id
                it.options = reject
                it.options.order = request.options.order
            }
        } else null
    }

    /**
     * 服务端尝试生成 NAK 响应（本实现不处理 NAK，直接返回 null）
     * @param request 收到的协商请求帧
     * @return null
     */
    override fun tryCreateServerNak(request: Ipv6cpConfigureFrame): Ipv6cpConfigureFrame? {
        return null
    }

    /**
     * 服务端生成 ACK 响应，直接确认客户端请求的选项
     * @param request 收到的协商请求帧
     * @return ACK 响应帧
     */
    override fun createServerAck(request: Ipv6cpConfigureFrame): Ipv6cpConfigureFrame {
        return Ipv6cpConfigureAck().also {
            it.id = request.id
            it.options = request.options
        }
    }

    /**
     * 客户端生成请求帧，包含 IPv6 标识符选项
     * @return 协商请求帧
     */
    override fun createClientRequest(): Ipv6cpConfigureFrame {
        val request = Ipv6cpConfigureRequest()

        request.options.identifierOption = Ipv6cpIdentifierOption().also {
            bridge.currentIPv6.copyInto(it.identifier)
        }

        return request
    }

    /**
     * 客户端处理收到的 NAK 响应，更新 IPv6 标识符
     * @param nak 收到的 NAK 响应帧
     */
    override suspend fun tryAcceptClientNak(nak: Ipv6cpConfigureFrame) {
        nak.options.identifierOption?.also {
            it.identifier.copyInto(bridge.currentIPv6)
        }
    }

    /**
     * 客户端处理收到的 REJECT 响应，IPv6 标识符被拒绝则发送错误消息
     * @param reject 收到的 REJECT 响应帧
     */
    override suspend fun tryAcceptClientReject(reject: Ipv6cpConfigureFrame) {
        reject.options.identifierOption?.also {
            bridge.controlMailbox.send(
                ControlMessage(Where.IPV6CP_IDENTIFIER, Result.ERR_OPTION_REJECTED)
            )
        }
    }
}
