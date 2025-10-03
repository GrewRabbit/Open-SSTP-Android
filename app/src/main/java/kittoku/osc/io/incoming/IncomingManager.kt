/**
 * 文件：app/src/main/java/kittoku/osc/io/incoming/IncomingManager.kt
 * 作用说明：
 * 本文件定义了 IncomingManager 类，负责管理 VPN 数据通道的入站数据处理，包括 SSTP 和 PPP 协议的数据包解析、心跳检测、各协议客户端的邮箱注册与注销等。
 * 主要功能包括：启动主入站协程，解析和分发收到的数据包，维护心跳定时器，管理各协议客户端的消息通道。
 * 在本软件项目中，VPN 会话管理模块会调用该文件，用于实现数据通道的入站数据处理和协议分发。
 * 本文件会调用 EchoTimer（心跳检测）、各 PPP/SSTP 客户端（如 LCPClient、PAPClient 等）、SharedBridge（会话桥接）、ControlMessage（控制消息）等模块。
 * 本文件依赖标准库 ByteBuffer、kotlinx.coroutines 及项目内协议相关单元和扩展方法。
 */

package kittoku.osc.io.incoming

import kittoku.osc.ControlMessage
import kittoku.osc.MAX_MRU
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.client.SstpClient
import kittoku.osc.client.ppp.IpcpClient
import kittoku.osc.client.ppp.Ipv6cpClient
import kittoku.osc.client.ppp.LCPClient
import kittoku.osc.client.ppp.PPPClient
import kittoku.osc.client.ppp.auth.ChapClient
import kittoku.osc.client.ppp.auth.EAPClient
import kittoku.osc.client.ppp.auth.PAPClient
import kittoku.osc.extension.probeByte
import kittoku.osc.extension.probeShort
import kittoku.osc.extension.toIntAsUShort
import kittoku.osc.unit.ppp.Frame
import kittoku.osc.unit.ppp.IpcpConfigureFrame
import kittoku.osc.unit.ppp.Ipv6cpConfigureFrame
import kittoku.osc.unit.ppp.LCPConfigureFrame
import kittoku.osc.unit.ppp.LCPEchoRequest
import kittoku.osc.unit.ppp.PPP_HDLC_HEADER
import kittoku.osc.unit.ppp.PPP_PROTOCOL_CHAP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_EAP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_IP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_IPCP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_IPv6
import kittoku.osc.unit.ppp.PPP_PROTOCOL_IPv6CP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_LCP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_PAP
import kittoku.osc.unit.ppp.auth.ChapFrame
import kittoku.osc.unit.ppp.auth.EAPFrame
import kittoku.osc.unit.ppp.auth.PAPFrame
import kittoku.osc.unit.sstp.ControlPacket
import kittoku.osc.unit.sstp.SSTP_PACKET_TYPE_CONTROL
import kittoku.osc.unit.sstp.SSTP_PACKET_TYPE_DATA
import kittoku.osc.unit.sstp.SstpEchoRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

private const val SSTP_ECHO_INTERVAL = 20_000L // SSTP 心跳检测间隔
private const val PPP_ECHO_INTERVAL = 20_000L // PPP 心跳检测间隔

/**
 * 入站数据管理器，负责解析和分发 VPN 入站数据包，维护心跳定时器及各协议邮箱
 * @param bridge 会话桥接对象，提供数据通道和控制通道
 */
internal class IncomingManager(internal val bridge: SharedBridge) {
    // 缓冲区大小，包含 SSL 应用缓冲区、最大 MRU 及碎片冗余
    private val bufferSize = bridge.sslTerminal!!.getApplicationBufferSize() + MAX_MRU + 8

    // 主入站协程任务
    private var jobMain: Job? = null

    // 各协议客户端的消息通道（邮箱）
    internal var lcpMailbox: Channel<LCPConfigureFrame>? = null
    internal var papMailbox: Channel<PAPFrame>? = null
    internal var chapMailbox: Channel<ChapFrame>? = null
    internal var eapMailbox: Channel<EAPFrame>? = null
    internal var ipcpMailbox: Channel<IpcpConfigureFrame>? = null
    internal var ipv6cpMailbox: Channel<Ipv6cpConfigureFrame>? = null
    internal var pppMailbox: Channel<Frame>? = null
    internal var sstpMailbox: Channel<ControlPacket>? = null

    // SSTP 心跳定时器
    private val sstpTimer = EchoTimer(SSTP_ECHO_INTERVAL) {
        SstpEchoRequest().also {
            bridge.sslTerminal!!.send(it.toByteBuffer())
        }
    }

    // PPP 心跳定时器
    private val pppTimer = EchoTimer(PPP_ECHO_INTERVAL) {
        LCPEchoRequest().also {
            it.id = bridge.allocateNewFrameID()
            it.holder = "Abura Mashi Mashi".toByteArray(Charsets.US_ASCII)
            bridge.sslTerminal!!.send(it.toByteBuffer())
        }
    }

    /**
     * 注册协议客户端的邮箱，便于数据分发
     * @param client 协议客户端对象
     */
    internal fun <T> registerMailbox(client: T) {
        when (client) {
            is LCPClient -> lcpMailbox = client.mailbox
            is PAPClient -> papMailbox = client.mailbox
            is ChapClient -> chapMailbox = client.mailbox
            is EAPClient -> eapMailbox = client.mailbox
            is IpcpClient -> ipcpMailbox = client.mailbox
            is Ipv6cpClient -> ipv6cpMailbox = client.mailbox
            is PPPClient -> pppMailbox = client.mailbox
            is SstpClient -> sstpMailbox = client.mailbox
            else -> throw NotImplementedError(client?.toString() ?: "")
        }
    }

    /**
     * 注销协议客户端的邮箱，断开数据分发
     * @param client 协议客户端对象
     */
    internal fun <T> unregisterMailbox(client: T) {
        when (client) {
            is LCPClient -> lcpMailbox = null
            is PAPClient -> papMailbox = null
            is ChapClient -> chapMailbox = null
            is EAPClient -> eapMailbox = null
            is IpcpClient -> ipcpMailbox = null
            is Ipv6cpClient -> ipv6cpMailbox = null
            is PPPClient -> pppMailbox = null
            is SstpClient -> sstpMailbox = null
            else -> throw NotImplementedError(client?.toString() ?: "")
        }
    }

    /**
     * 启动主入站数据处理协程，负责接收、解析和分发数据包，并进行心跳检测
     */
    internal fun launchJobMain() {
        jobMain = bridge.service.scope.launch(bridge.handler) {
            val buffer = ByteBuffer.allocate(bufferSize).also { it.limit(0) }

            sstpTimer.tick()
            pppTimer.tick()

            while (isActive) {
                // 检查 SSTP 心跳
                if (!sstpTimer.checkAlive()) {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_CONTROL, Result.ERR_TIMEOUT)
                    )
                    return@launch
                }

                // 检查 PPP 心跳
                if (!pppTimer.checkAlive()) {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.PPP, Result.ERR_TIMEOUT)
                    )
                    return@launch
                }

                // 获取数据包长度
                val size = getPacketSize(buffer)
                when (size) {
                    in 4..bufferSize -> { /* 合法包，继续处理 */ }
                    -1 -> {
                        bridge.sslTerminal!!.receive(buffer)
                        continue
                    }
                    else -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.INCOMING, Result.ERR_INVALID_PACKET_SIZE)
                        )
                        return@launch
                    }
                }

                // 检查缓冲区剩余空间
                if (size > buffer.remaining()) {
                    bridge.sslTerminal!!.receive(buffer)
                    continue
                }

                sstpTimer.tick()

                // 根据包类型分发处理
                when (buffer.probeShort(0)) {
                    SSTP_PACKET_TYPE_DATA -> {
                        if (buffer.probeShort(4) != PPP_HDLC_HEADER) {
                            bridge.controlMailbox.send(
                                ControlMessage(Where.SSTP_DATA, Result.ERR_UNKNOWN_TYPE)
                            )
                            return@launch
                        }

                        pppTimer.tick()

                        val protocol = buffer.probeShort(6)

                        // DATA 包处理
                        if (protocol == PPP_PROTOCOL_IP) {
                            processIPPacket(bridge.PPP_IPv4_ENABLED, size, buffer)
                            continue
                        }
                        if (protocol == PPP_PROTOCOL_IPv6) {
                            processIPPacket(bridge.PPP_IPv6_ENABLED, size, buffer)
                            continue
                        }

                        // CONTROL 包处理
                        val code = buffer.probeByte(8)
                        val isGo = when (protocol) {
                            PPP_PROTOCOL_LCP -> processLcpFrame(code, buffer)
                            PPP_PROTOCOL_PAP -> processPAPFrame(code, buffer)
                            PPP_PROTOCOL_CHAP -> processChapFrame(code, buffer)
                            PPP_PROTOCOL_EAP -> processEAPFrame(code, buffer)
                            PPP_PROTOCOL_IPCP -> processIpcpFrame(code, buffer)
                            PPP_PROTOCOL_IPv6CP -> processIpv6cpFrame(code, buffer)
                            else -> processUnknownProtocol(protocol, size, buffer)
                        }

                        if (!isGo) {
                            return@launch
                        }
                    }

                    SSTP_PACKET_TYPE_CONTROL -> {
                        if (!processControlPacket(buffer.probeShort(4), buffer)) {
                            return@launch
                        }
                    }

                    else -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.INCOMING, Result.ERR_UNKNOWN_TYPE)
                        )
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * 获取当前缓冲区中的数据包长度
     * @param buffer 数据缓冲区
     * @return 包长度，-1 表示数据不足
     */
    private fun getPacketSize(buffer: ByteBuffer): Int {
        return if (buffer.remaining() < 4) {
            -1
        } else {
            buffer.probeShort(2).toIntAsUShort()
        }
    }

    /**
     * 取消主入站数据处理协程
     */
    internal fun cancel() {
        jobMain?.cancel()
    }
}