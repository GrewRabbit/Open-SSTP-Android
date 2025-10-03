/**
 * 文件：app/src/main/java/kittoku/osc/io/OutgoingManager.kt
 * 作用说明：
 * 本文件定义了 OutgoingManager 类，负责管理 VPN 数据通道的出站数据处理，包括从 IP 层读取数据包、组装 SSTP/PPP 协议数据包并发送到 SSL 通道。
 * 主要功能包括：启动主出站协程、从 IP 终端检索数据、协议类型判断与组包、发送数据到远端。
 * 在本项目中，VPN 会话管理模块会调用该文件，用于实现数据通道的出站数据处理和协议组包。
 * 本文件会调用 SharedBridge（会话桥接）、ControlMessage（控制消息）、SSL 终端、IP 终端等模块。
 * 依赖标准库 ByteBuffer、kotlinx.coroutines 及项目内协议相关单元。
 */

package kittoku.osc.io

import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.unit.ppp.PPP_HDLC_HEADER
import kittoku.osc.unit.ppp.PPP_PROTOCOL_IP
import kittoku.osc.unit.ppp.PPP_PROTOCOL_IPv6
import kittoku.osc.unit.sstp.SSTP_PACKET_TYPE_DATA
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

private const val PREFIX_SIZE = 8

private const val IPv4_VERSION_HEADER: Int = (0x4).shl(4 + 3 * Byte.SIZE_BITS)
private const val IPv6_VERSION_HEADER: Int = (0x6).shl(4 + 3 * Byte.SIZE_BITS)
private const val IP_VERSION_MASK: Int = (0xF).shl(4 + 3 * Byte.SIZE_BITS)

/**
 * 出站数据管理器，负责从 IP 层读取数据并组装为 SSTP/PPP 协议包发送到远端
 * @param bridge 会话桥接对象，提供数据通道和控制通道
 */
internal class OutgoingManager(private val bridge: SharedBridge) {
    // 主出站协程任务
    private var jobMain: Job? = null
    // IP 数据检索协程任务
    private var jobRetrieve: Job? = null

    // 主缓冲区，用于组装待发送的数据包
    private val mainBuffer = ByteBuffer.allocate(bridge.sslTerminal!!.getApplicationBufferSize())
    // 协程间通信通道，用于传递 IP 层数据包
    private val channel = Channel<ByteBuffer>(0)

    /**
     * 启动主出站数据处理协程，负责组装和发送数据包
     */
    internal fun launchJobMain() {
        jobMain = bridge.service.scope.launch(bridge.handler) {
            launchJobRetrieve()

            val minCapacity = PREFIX_SIZE + bridge.PPP_MTU

            while (isActive) {
                mainBuffer.clear()

                // 从 channel 接收 IP 层数据包并组装
                if (!load(channel.receive())) continue

                // 批量处理 channel 中剩余数据包
                while (isActive) {
                    channel.tryReceive().getOrNull()?.also {
                        load(it)
                    } ?: break

                    if (mainBuffer.remaining() < minCapacity) break
                }

                mainBuffer.flip()
                bridge.sslTerminal!!.send(mainBuffer)
            }
        }
    }

    /**
     * 启动 IP 数据检索协程，从 IP 终端读取数据包并发送到 channel
     */
    private fun launchJobRetrieve() {
        jobRetrieve = bridge.service.scope.launch(bridge.handler) {
            val bufferAlpha = ByteBuffer.allocate(bridge.PPP_MTU)
            val bufferBeta = ByteBuffer.allocate(bridge.PPP_MTU)
            var isBlockingAlpha = true

            while (isActive) {
                isBlockingAlpha = if (isBlockingAlpha) {
                    bridge.ipTerminal!!.readPacket(bufferAlpha)
                    channel.send(bufferAlpha)
                    false
                } else {
                    bridge.ipTerminal!!.readPacket(bufferBeta)
                    channel.send(bufferBeta)
                    true
                }
            }
        }
    }

    /**
     * 组装 IP 层数据包为 SSTP/PPP 协议包
     * @param packet IP 层数据包
     * @return 是否启用对应协议，未启用则丢弃
     */
    private suspend fun load(packet: ByteBuffer): Boolean {
        val header = packet.getInt(0)
        val protocol = when (header and IP_VERSION_MASK) {
            IPv4_VERSION_HEADER -> {
                if (!bridge.PPP_IPv4_ENABLED) return false
                PPP_PROTOCOL_IP
            }
            IPv6_VERSION_HEADER -> {
                if (!bridge.PPP_IPv6_ENABLED) return false
                PPP_PROTOCOL_IPv6
            }
            else -> {
                bridge.controlMailbox.send(ControlMessage(Where.OUTGOING, Result.ERR_UNKNOWN_TYPE))
                return false
            }
        }

        // 组装 SSTP/PPP 协议包头
        mainBuffer.putShort(SSTP_PACKET_TYPE_DATA)
        mainBuffer.putShort((packet.remaining() + PREFIX_SIZE).toShort())
        mainBuffer.putShort(PPP_HDLC_HEADER)
        mainBuffer.putShort(protocol)
        mainBuffer.put(packet)

        return true
    }

    /**
     * 取消所有出站相关协程任务并关闭通道
     */
    internal fun cancel() {
        jobMain?.cancel()
        jobRetrieve?.cancel()
        channel.close()
    }
}