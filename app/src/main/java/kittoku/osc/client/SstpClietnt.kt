/**
 * 文件：app/src/main/java/kittoku/osc/client/SstpClietnt.kt
 * 作用说明：
 * 本文件实现了 SSTP（Secure Socket Tunneling Protocol）客户端 SstpClient，负责 SSTP 协议的连接建立、控制消息处理、认证和断开流程。
 * 在本软件项目中，SSTP VPN 连接建立和维护过程中会调用该文件，主要由主流程控制模块（如 PPPManager 或连接管理类）实例化和调用 SstpClient，以完成 SSTP 协议的协商和控制。
 * 本文件会调用 SharedBridge（通信桥）、ControlMessage（控制消息）、Result（结果类型）、Where（流程位置标识）、以及 SSTP 协议相关的 ControlPacket、SstpCallConnectRequest、SstpCallConnectAck 等数据结构，负责与底层 SSTP 协议和消息交互。
 * SstpClient 负责 SSTP 握手、认证、心跳、断开等控制流程，提升了 SSTP 协议的模块化和健壮性。
 */

package kittoku.osc.client

import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.preference.AUTH_PROTOCOL_EAP_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOL_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOl_PAP
import kittoku.osc.unit.sstp.CERT_HASH_PROTOCOL_SHA1
import kittoku.osc.unit.sstp.CERT_HASH_PROTOCOL_SHA256
import kittoku.osc.unit.sstp.ControlPacket
import kittoku.osc.unit.sstp.SSTP_MESSAGE_TYPE_CALL_ABORT
import kittoku.osc.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT
import kittoku.osc.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
import kittoku.osc.unit.sstp.SstpCallAbort
import kittoku.osc.unit.sstp.SstpCallConnectAck
import kittoku.osc.unit.sstp.SstpCallConnectNak
import kittoku.osc.unit.sstp.SstpCallConnectRequest
import kittoku.osc.unit.sstp.SstpCallConnected
import kittoku.osc.unit.sstp.SstpCallDisconnect
import kittoku.osc.unit.sstp.SstpCallDisconnectAck
import kittoku.osc.unit.sstp.SstpEchoRequest
import kittoku.osc.unit.sstp.SstpEchoResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// SSTP 握手请求的时间间隔和最大重试次数
private const val SSTP_REQUEST_INTERVAL = 60_000L
private const val SSTP_REQUEST_COUNT = 3
internal const val SSTP_REQUEST_TIMEOUT = SSTP_REQUEST_INTERVAL * SSTP_REQUEST_COUNT

/**
 * 哈希算法设置类，根据协商的 hashProtocol 选择摘要和 MAC 算法
 */
private class HashSetting(hashProtocol: Byte) {
    val cmacSize: Short // 小端序
    val digestProtocol: String
    val macProtocol: String

    init {
        when (hashProtocol) {
            CERT_HASH_PROTOCOL_SHA1 -> {
                cmacSize = 0x1400.toShort()
                digestProtocol = "SHA-1"
                macProtocol = "HmacSHA1"
            }
            CERT_HASH_PROTOCOL_SHA256 -> {
                cmacSize = 0x2000.toShort()
                digestProtocol = "SHA-256"
                macProtocol = "HmacSHA256"
            }
            else -> throw NotImplementedError(hashProtocol.toString())
        }
    }
}

/**
 * SSTP 客户端主类，负责 SSTP 协议的连接、控制、认证和断开流程
 */
internal class SstpClient(val bridge: SharedBridge) {
    // 控制消息收发邮箱
    internal val mailbox = Channel<ControlPacket>(Channel.BUFFERED)

    // 握手和控制任务 Job
    private var jobRequest: Job? = null
    private var jobControl: Job? = null

    /**
     * 启动 SSTP 控制任务，循环处理收到的控制帧并做出响应
     */
    internal suspend fun launchJobControl() {
        jobControl = bridge.service.scope.launch(bridge.handler) {
            while (isActive) {
                when (mailbox.receive()) {
                    is SstpEchoRequest -> { // 处理心跳请求，回复 EchoResponse
                        SstpEchoResponse().also {
                            bridge.sslTerminal!!.send(it.toByteBuffer())
                        }
                    }
                    is SstpEchoResponse -> { } // 收到心跳响应，无需处理
                    is SstpCallDisconnect -> { // 处理断开请求
                        bridge.controlMailbox.send(
                            ControlMessage(Where.SSTP_CONTROL, Result.ERR_DISCONNECT_REQUESTED)
                        )
                    }
                    is SstpCallAbort -> { // 处理中止请求
                        bridge.controlMailbox.send(
                            ControlMessage(Where.SSTP_CONTROL, Result.ERR_ABORT_REQUESTED)
                        )
                    }
                    else -> { // 处理未知消息
                        bridge.controlMailbox.send(
                            ControlMessage(Where.SSTP_CONTROL, Result.ERR_UNEXPECTED_MESSAGE)
                        )
                    }
                }
            }
        }
    }

    /**
     * 启动 SSTP 握手请求任务，负责连接请求和响应处理
     */
    internal suspend fun launchJobRequest() {
        jobRequest = bridge.service.scope.launch(bridge.handler) {
            val request = SstpCallConnectRequest()
            var requestCount = SSTP_REQUEST_COUNT

            val received: ControlPacket
            while (true) {
                requestCount--
                if (requestCount < 0) {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_COUNT_EXHAUSTED)
                    )
                    return@launch
                }

                bridge.sslTerminal!!.send(request.toByteBuffer())
                received = withTimeoutOrNull(SSTP_REQUEST_INTERVAL) { mailbox.receive() } ?: continue
                break
            }

            // 处理握手响应
            when (received) {
                is SstpCallConnectAck -> {
                    bridge.hashProtocol = when (received.request.bitmask.toInt()) {
                        in 2..3 -> CERT_HASH_PROTOCOL_SHA256
                        1 -> CERT_HASH_PROTOCOL_SHA1
                        else -> {
                            bridge.controlMailbox.send(
                                ControlMessage(Where.SSTP_HASH, Result.ERR_UNKNOWN_TYPE)
                            )
                            return@launch
                        }
                    }
                    received.request.nonce.copyInto(bridge.nonce)
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.PROCEEDED)
                    )
                }
                is SstpCallConnectNak -> {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_NEGATIVE_ACKNOWLEDGED)
                    )
                }
                is SstpCallDisconnect -> {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_DISCONNECT_REQUESTED)
                    )
                }
                is SstpCallAbort -> {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_ABORT_REQUESTED)
                    )
                }
                else -> {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_UNEXPECTED_MESSAGE)
                    )
                }
            }
        }
    }

    /**
     * 发送 CallConnected 消息，完成认证和 MAC 校验
     */
    internal suspend fun sendCallConnected() {
        val call = SstpCallConnected()
        val cmkInputBuffer = ByteBuffer.allocate(32)
        val cmacInputBuffer = ByteBuffer.allocate(call.length)
        val hashSetting = HashSetting(bridge.hashProtocol)

        bridge.nonce.copyInto(call.binding.nonce)
        MessageDigest.getInstance(hashSetting.digestProtocol).also {
            it.digest(bridge.sslTerminal!!.getServerCertificate()).copyInto(call.binding.certHash)
        }
        call.binding.hashProtocol = bridge.hashProtocol
        call.write(cmacInputBuffer)

        val hlak = when (bridge.currentAuth) {
            AUTH_PROTOCOl_PAP -> ByteArray(32)
            AUTH_PROTOCOL_MSCHAPv2, AUTH_PROTOCOL_EAP_MSCHAPv2 -> bridge.hlak!!
            else -> throw NotImplementedError(bridge.currentAuth)
        }

        val cmkSeed = "SSTP inner method derived CMK".toByteArray(Charset.forName("US-ASCII"))
        cmkInputBuffer.put(cmkSeed)
        cmkInputBuffer.putShort(hashSetting.cmacSize)
        cmkInputBuffer.put(1)

        Mac.getInstance(hashSetting.macProtocol).also {
            it.init(SecretKeySpec(hlak, hashSetting.macProtocol))
            val cmk = it.doFinal(cmkInputBuffer.array())
            it.init(SecretKeySpec(cmk, hashSetting.macProtocol))
            val cmac = it.doFinal(cmacInputBuffer.array())
            cmac.copyInto(call.binding.compoundMac)
        }

        bridge.sslTerminal!!.send(call.toByteBuffer())
    }

    /**
     * 发送断开或中止等最后一个控制包
     * @param type 控制包类型
     */
    internal suspend fun sendLastPacket(type: Short) {
        val packet = when (type) {
            SSTP_MESSAGE_TYPE_CALL_DISCONNECT -> SstpCallDisconnect()
            SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK -> SstpCallDisconnectAck()
            SSTP_MESSAGE_TYPE_CALL_ABORT -> SstpCallAbort()
            else -> throw NotImplementedError(type.toString())
        }

        try { // 可能 socket 已不可用
            bridge.sslTerminal!!.send(packet.toByteBuffer())
        } catch (_: Throwable) { }
    }

    /**
     * 取消所有任务并关闭邮箱
     */
    internal fun cancel() {
        jobRequest?.cancel()
        jobControl?.cancel()
        mailbox.close()
    }
}