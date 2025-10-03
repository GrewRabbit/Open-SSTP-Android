/**
 * 文件：ConfigClient.kt
 * 作用说明：
 * 本文件定义了 PPP 协议协商阶段的抽象客户端 `ConfigClient`，用于处理 PPP 配置请求、应答、重试等协商流程。
 * 在本软件项目中，PPP 协议相关的模块（如 LCP、IPCP、PAP、CHAP 等）会继承并实现该类，完成具体的协商逻辑。
 * 其他 PPP 客户端实现文件会调用本文件，主要通过继承和调用 `launchJobNegotiation` 方法启动协商流程。
 * 本文件会调用 `SharedBridge`、`ControlMessage`、`Frame` 及相关 PPP 协议常量和工具类，负责与底层通信和消息分发。
 */

package kittoku.osc.client.ppp

import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.unit.ppp.Frame
import kittoku.osc.unit.ppp.LCP_CODE_CONFIGURE_ACK
import kittoku.osc.unit.ppp.LCP_CODE_CONFIGURE_NAK
import kittoku.osc.unit.ppp.LCP_CODE_CONFIGURE_REJECT
import kittoku.osc.unit.ppp.LCP_CODE_CONFIGURE_REQUEST
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// PPP 协商请求的时间间隔和最大重试次数
private const val PPP_REQUEST_INTERVAL = 3000L
private const val PPP_REQUEST_COUNT = 10
internal const val PPP_NEGOTIATION_TIMEOUT = PPP_REQUEST_INTERVAL * PPP_REQUEST_COUNT

/**
 * PPP 协商客户端抽象类，负责处理 PPP 协议的配置请求与应答流程。
 * @param where 标识协商位置
 * @param bridge PPP 通信桥接对象
 */
internal abstract class ConfigClient<T: Frame>(private val where: Where, protected val bridge: SharedBridge) {
    // 协商消息收发邮箱
    internal val mailbox = Channel<T>(Channel.BUFFERED)

    // 协商任务 Job
    private var jobNegotiation: Job? = null

    // 请求 ID，仅客户端使用
    private var requestID: Byte = 0
    // 剩余请求次数
    private var requestCount = PPP_REQUEST_COUNT

    // 客户端和服务端协商状态
    private var isClientReady = false
    private var isServerReady = false

    // 协商是否完成
    private val isOpen: Boolean
        get() = isClientReady && isServerReady

    /**
     * 消耗请求计数器，超限时发送错误消息
     */
    private suspend fun consumeRequestCounter() {
        requestCount--
        if (requestCount < 0) {
            bridge.controlMailbox.send(ControlMessage(where, Result.ERR_COUNT_EXHAUSTED))
        }
    }

    /**
     * 服务端尝试生成 REJECT 响应
     */
    protected abstract fun tryCreateServerReject(request: T): T?

    /**
     * 服务端尝试生成 NAK 响应
     */
    protected abstract fun tryCreateServerNak(request: T): T?

    /**
     * 服务端生成 ACK 响应
     */
    protected abstract fun createServerAck(request: T): T

    /**
     * 客户端生成请求帧
     */
    protected abstract fun createClientRequest(): T

    /**
     * 客户端处理收到的 REJECT 响应
     */
    protected abstract suspend fun tryAcceptClientReject(reject: T)

    /**
     * 客户端处理收到的 NAK 响应
     */
    protected abstract suspend fun tryAcceptClientNak(nak: T)

    /**
     * 发送客户端请求帧
     */
    private suspend fun sendClientRequest() {
        consumeRequestCounter()
        requestID = bridge.allocateNewFrameID()
        createClientRequest().also {
            it.id = requestID
            bridge.sslTerminal!!.send(it.toByteBuffer())
        }
    }

    /**
     * 启动 PPP 协商任务，处理请求与应答的主循环
     */
    internal fun launchJobNegotiation() {
        jobNegotiation = bridge.service.scope.launch(bridge.handler) {
            sendClientRequest()
            while (isActive) {
                val tried  = withTimeoutOrNull(PPP_REQUEST_INTERVAL) { mailbox.receive() }
                val received: T
                if (tried == null) {
                    isClientReady = false
                    sendClientRequest()
                    continue
                } else {
                    received = tried
                }

                // 处理服务端请求
                if (received.code == LCP_CODE_CONFIGURE_REQUEST) {
                    isServerReady = false
                    val reject = tryCreateServerReject(received)
                    if (reject != null) {
                        bridge.sslTerminal!!.send(reject.toByteBuffer())
                        continue
                    }
                    val nak = tryCreateServerNak(received)
                    if (nak != null) {
                        bridge.sslTerminal!!.send(nak.toByteBuffer())
                        continue
                    }
                    createServerAck(received).also {
                        bridge.sslTerminal!!.send(it.toByteBuffer())
                        isServerReady = true
                    }
                } else {
                    // 处理客户端请求结果
                    if (isClientReady) {
                        isClientReady = false
                        sendClientRequest()
                        continue
                    }
                    if (received.id != requestID) {
                        continue
                    }
                    when(received.code) {
                        LCP_CODE_CONFIGURE_ACK -> {
                            isClientReady = true
                        }
                        LCP_CODE_CONFIGURE_NAK -> {
                            tryAcceptClientNak(received)
                            sendClientRequest()
                        }
                        LCP_CODE_CONFIGURE_REJECT -> {
                            tryAcceptClientReject(received)
                            sendClientRequest()
                        }
                    }
                }

                // 协商完成，发送通知
                if (isOpen) {
                    requestCount = PPP_REQUEST_COUNT
                    bridge.controlMailbox.send(ControlMessage(where, Result.PROCEEDED))
                    break
                }
            }
        }
    }

    /**
     * 取消协商任务并关闭邮箱
     */
    internal fun cancel() {
        jobNegotiation?.cancel()
        mailbox.close()
    }
}
