/**
 * 文件：PPPClient.kt
 * 作用说明：
 * 本文件实现了 PPP 协议的通用客户端 PPPClient，负责处理 PPP 控制消息（如 Echo、终止、协议拒绝等）的收发与响应。
 * 在本软件项目中，PPP 链路建立和维护过程中会调用该文件，主要由 PPP 主流程控制模块（如 PPPManager 或连接管理类）实例化和调用 PPPClient，以处理底层 PPP 控制帧。
 * 本文件会调用 SharedBridge（通信桥）、ControlMessage（控制消息）、Frame 及各类 PPP 控制帧（如 LCPEchoRequest、LCPTerminalRequest 等），用于与底层 PPP 协议和消息交互。
 * 作为 PPP 协议控制阶段的具体实现，负责链路状态监控、异常处理和协议交互，提高了 PPP 协议的健壮性和模块化。
 */

package kittoku.osc.client.ppp

import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.unit.ppp.Frame
import kittoku.osc.unit.ppp.LCPCodeReject
import kittoku.osc.unit.ppp.LCPEchoReply
import kittoku.osc.unit.ppp.LCPEchoRequest
import kittoku.osc.unit.ppp.LCPProtocolReject
import kittoku.osc.unit.ppp.LCPTerminalAck
import kittoku.osc.unit.ppp.LCPTerminalRequest
import kittoku.osc.unit.ppp.LcpDiscardRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// PPP 协议通用客户端，负责处理 PPP 控制消息
internal class PPPClient(val bridge: SharedBridge) {
    // 控制消息收发邮箱
    internal val mailbox = Channel<Frame>(Channel.BUFFERED)

    // 控制任务 Job
    private var jobControl: Job? = null

    /**
     * 启动 PPP 控制任务，循环处理收到的控制帧并做出响应
     */
    internal suspend fun launchJobControl() {
        jobControl = bridge.service.scope.launch(bridge.handler) {
            while (isActive) {
                when (val received = mailbox.receive()) {
                    // 处理 Echo 请求，回复 Echo Reply
                    is LCPEchoRequest -> {
                        LCPEchoReply().also {
                            it.id = received.id
                            it.holder = "Abura Mashi Mashi".toByteArray(Charsets.US_ASCII)
                            bridge.sslTerminal!!.send(it.toByteBuffer())
                        }
                    }

                    // 收到 Echo Reply，无需处理
                    is LCPEchoReply -> { }

                    // 收到 Discard Request，无需处理
                    is LcpDiscardRequest -> { }

                    // 处理终止请求，回复终止确认并发送错误消息
                    is LCPTerminalRequest -> {
                        LCPTerminalAck().also {
                            it.id = received.id
                            bridge.sslTerminal!!.send(it.toByteBuffer())
                        }

                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_TERMINATE_REQUESTED)
                        )
                    }

                    // 处理协议拒绝，发送错误消息
                    is LCPProtocolReject -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_PROTOCOL_REJECTED)
                        )
                    }

                    // 处理代码拒绝，发送错误消息
                    is LCPCodeReject -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_CODE_REJECTED)
                        )
                    }

                    // 处理其他未知消息，发送通用错误
                    else -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_UNEXPECTED_MESSAGE)
                        )
                    }
                }
            }
        }
    }

    /**
     * 取消控制任务并关闭邮箱
     */
    internal fun cancel() {
        jobControl?.cancel()
        mailbox.close()
    }
}