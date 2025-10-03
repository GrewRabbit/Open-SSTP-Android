/**
 * 文件：app/src/main/java/kittoku/osc/unit/sstp/ControlPacket.kt
 * 作用说明：
 * 本文件实现了SSTP（Secure Socket Tunneling Protocol）控制包的数据结构及其解析与构建方法，
 * 负责SSTP连接建立、维护、终止等过程中的控制消息（如连接请求、确认、断开、回显等）的封装和处理。
 * 主要功能：
 * - ControlPacket：SSTP控制包抽象基类，统一头部读写接口。
 * - SstpCallConnectRequest/Ack/Nak/Connected：连接相关控制包，包含协议ID、加密绑定等属性。
 * - SstpCallAbort/Disconnect/DisconnectAck：连接终止相关控制包，包含状态信息属性。
 * - SstpEchoRequest/Response：回显检测控制包，无属性。
 * 在本项目中，SSTP数据包解析器、状态机、连接管理器等模块会调用本文件，
 * 用于处理SSTP控制消息的收发和协议流程控制。
 * 本文件会调用`Attribute`相关实现（如`EncapsulatedProtocolId`、`StatusInfo`、`CryptoBinding`等），
 * 以及`DataUnit`基类和字节处理工具方法（如`assertAlways`、`toIntAsUShort`）。
 * 其它SSTP协议实现文件（如`Packet.kt`、`StateMachine.kt`等）会依赖本文件，实现SSTP控制包的管理和协议交互。
 */

package kittoku.osc.unit.sstp

import kittoku.osc.debug.ParsingDataUnitException
import kittoku.osc.debug.assertAlways
import kittoku.osc.extension.toIntAsUShort
import kittoku.osc.unit.DataUnit
import java.nio.ByteBuffer

// SSTP包类型和消息类型常量定义
internal const val SSTP_PACKET_TYPE_DATA: Short = 0x1000
internal const val SSTP_PACKET_TYPE_CONTROL: Short = 0x1001

internal const val SSTP_MESSAGE_TYPE_CALL_CONNECT_REQUEST: Short = 1
internal const val SSTP_MESSAGE_TYPE_CALL_CONNECT_ACK: Short = 2
internal const val SSTP_MESSAGE_TYPE_CALL_CONNECT_NAK: Short = 3
internal const val SSTP_MESSAGE_TYPE_CALL_CONNECTED: Short = 4
internal const val SSTP_MESSAGE_TYPE_CALL_ABORT: Short = 5
internal const val SSTP_MESSAGE_TYPE_CALL_DISCONNECT: Short = 6
internal const val SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK: Short = 7
internal const val SSTP_MESSAGE_TYPE_ECHO_REQUEST: Short = 8
internal const val SSTP_MESSAGE_TYPE_ECHO_RESPONSE: Short = 9

/**
 * SSTP控制包抽象基类
 * 统一头部读写接口
 */
internal abstract class ControlPacket : DataUnit() {
    internal abstract val type: Short // 控制包类型
    internal abstract val numAttribute: Int // 属性数量

    protected var givenLength = 0 // 包实际长度
    protected var givenNumAttribute = 0 // 实际属性数量

    /**
     * 读取控制包头部
     */
    protected fun readHeader(buffer: ByteBuffer) {
        assertAlways(buffer.short == SSTP_PACKET_TYPE_CONTROL)
        givenLength = buffer.short.toIntAsUShort()
        assertAlways(buffer.short == type)
        givenNumAttribute = buffer.short.toIntAsUShort()
    }

    /**
     * 写入控制包头部
     */
    protected fun writeHeader(buffer: ByteBuffer) {
        buffer.putShort(SSTP_PACKET_TYPE_CONTROL)
        buffer.putShort(length.toShort())
        buffer.putShort(type)
        buffer.putShort(numAttribute.toShort())
    }
}

/**
 * 连接请求包
 * 包含封装协议ID属性
 */
internal class SstpCallConnectRequest : ControlPacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_CONNECT_REQUEST
    override val length = 14
    override val numAttribute = 1

    internal var protocol = EncapsulatedProtocolId()

    /**
     * 读取连接请求包内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        assertAlways(givenLength == length)
        assertAlways(givenNumAttribute == numAttribute)
        protocol.read(buffer)
    }

    /**
     * 写入连接请求包内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        protocol.write(buffer)
    }
}

/**
 * 连接确认包
 * 包含加密绑定请求属性
 */
internal class SstpCallConnectAck : ControlPacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_CONNECT_ACK
    override val length = 48
    override val numAttribute = 1

    internal var request = CryptoBindingRequest()

    /**
     * 读取连接确认包内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        assertAlways(givenLength == length)
        assertAlways(givenNumAttribute == numAttribute)
        request.read(buffer)
    }

    /**
     * 写入连接确认包内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        request.write(buffer)
    }
}

/**
 * 连接拒绝包
 * 可包含多个状态信息属性和附加数据
 */
internal class SstpCallConnectNak : ControlPacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_CONNECT_NAK
    override val length: Int
        get() = 8 + statusInfos.fold(0) {sum, info -> sum + info.length } + holder.size

    override val numAttribute: Int
        get() = statusInfos.size

    internal val statusInfos = mutableListOf<StatusInfo>()
    internal var holder = ByteArray(0)

    /**
     * 读取连接拒绝包内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        repeat(givenNumAttribute) {
            StatusInfo().also {
                it.read(buffer)
                statusInfos.add(it)
            }
        }
        val holderSize = givenLength - length
        assertAlways(holderSize >= 0)
        if (holderSize > 0) {
            holder = ByteArray(holderSize).also { buffer.get(it) }
        }
    }

    /**
     * 写入连接拒绝包内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        statusInfos.forEach {
            it.write(buffer)
        }
        buffer.put(holder)
    }
}

/**
 * 连接建立包
 * 包含加密绑定属性
 */
internal class SstpCallConnected : ControlPacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_CONNECTED
    override val length = 112
    override val numAttribute = 1

    internal var binding = CryptoBinding()

    /**
     * 读取连接建立包内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        assertAlways(givenLength == length)
        assertAlways(givenNumAttribute == numAttribute)
        binding.read(buffer)
    }

    /**
     * 写入连接建立包内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        binding.write(buffer)
    }
}

/**
 * 连接终止相关包抽象类
 * 可包含状态信息属性
 */
internal abstract class TerminatePacket : ControlPacket() {
    override val length: Int
        get() = 8 + (statusInfo?.length ?: 0)

    override val numAttribute: Int
        get() = statusInfo?.let { 1 } ?: 0

    internal var statusInfo: StatusInfo? = null

    /**
     * 读取终止包内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        statusInfo = when (givenNumAttribute) {
            0 -> null
            1 -> StatusInfo().also { it.read(buffer) }
            else -> throw ParsingDataUnitException()
        }
        assertAlways(givenLength == length)
    }

    /**
     * 写入终止包内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        statusInfo?.also {
            it.write(buffer)
        }
    }
}

/**
 * 连接中止包
 */
internal class SstpCallAbort : TerminatePacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_ABORT
}

/**
 * 连接断开包
 */
internal class SstpCallDisconnect : TerminatePacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_DISCONNECT
}

/**
 * 无属性控制包抽象类
 * 用于断开确认、回显请求/应答等
 */
internal abstract class NoAttributePacket : ControlPacket() {
    override val length = 8
    override val numAttribute = 0

    /**
     * 读取无属性包内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
    }

    /**
     * 写入无属性包内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
    }
}

/**
 * 断开确认包
 */
internal class SstpCallDisconnectAck : NoAttributePacket() {
    override val type = SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
}

/**
 * 回显请求包
 */
internal class SstpEchoRequest : NoAttributePacket() {
    override val type = SSTP_MESSAGE_TYPE_ECHO_REQUEST
}

/**
 * 回显应答包
 */
internal class SstpEchoResponse : NoAttributePacket() {
    override val type = SSTP_MESSAGE_TYPE_ECHO_RESPONSE
}