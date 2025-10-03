/**
 * 文件：app/src/main/java/kittoku/osc/unit/sstp/Attribute.kt
 * 作用说明：
 * 本文件定义了SSTP（Secure Socket Tunneling Protocol）连接过程中使用的属性（Attribute）及其具体实现类，
 * 负责SSTP数据包属性的解析、构建和序列化，包括封装协议ID、状态信息、加密绑定等。
 * 主要功能：
 * - Attribute：SSTP属性抽象基类，统一属性头部读写接口。
 * - EncapsulatedProtocolId：封装协议ID属性，标识PPP等协议类型。
 * - StatusInfo：状态信息属性，传递连接状态及附加数据。
 * - CryptoBinding：加密绑定属性，包含哈希协议、随机数、证书哈希、MAC等。
 * - CryptoBindingRequest：加密绑定请求属性，包含随机数和请求标志。
 * 在本项目中，SSTP数据包解析器、SSTP连接控制器等模块会调用本文件，
 * 用于处理SSTP连接建立和维护过程中的属性读写。
 * 本文件会调用`DataUnit`基类、字节处理工具方法（如`assertAlways`、`move`、`padZeroByte`、`toIntAsUShort`）。
 * 其它SSTP相关实现文件（如`Packet.kt`、`StateMachine.kt`等）会依赖本文件，实现SSTP属性的管理和协议交互。
 */

package kittoku.osc.unit.sstp

import kittoku.osc.debug.assertAlways
import kittoku.osc.extension.move
import kittoku.osc.extension.padZeroByte
import kittoku.osc.extension.toIntAsUShort
import kittoku.osc.unit.DataUnit
import java.nio.ByteBuffer

// SSTP属性ID常量定义
internal const val SSTP_ATTRIBUTE_ID_NO_ERROR: Byte = 0
internal const val SSTP_ATTRIBUTE_ID_ENCAPSULATED_PROTOCOL_ID: Byte = 1
internal const val SSTP_ATTRIBUTE_ID_STATUS_INFO: Byte = 2
internal const val SSTP_ATTRIBUTE_ID_CRYPTO_BINDING: Byte = 3
internal const val SSTP_ATTRIBUTE_ID_CRYPTO_BINDING_REQ: Byte = 4

// 证书哈希协议类型常量
internal const val CERT_HASH_PROTOCOL_SHA1: Byte = 1
internal const val CERT_HASH_PROTOCOL_SHA256: Byte = 2

/**
 * SSTP属性抽象基类
 * 统一属性头部读写接口
 */
internal abstract class Attribute : DataUnit() {
    internal abstract val id: Byte // 属性类型ID
    protected var givenLength = 0 // 属性实际长度

    /**
     * 读取属性头部（类型ID和长度）
     */
    protected fun readHeader(buffer: ByteBuffer) {
        buffer.move(1)
        assertAlways(buffer.get() == id)
        givenLength = buffer.short.toIntAsUShort()
    }

    /**
     * 写入属性头部（类型ID和长度）
     */
    protected fun writeHeader(buffer: ByteBuffer) {
        buffer.put(0)
        buffer.put(id)
        buffer.putShort(length.toShort())
    }
}

/**
 * 封装协议ID属性
 * 标识SSTP封装的协议类型（如PPP）
 */
internal class EncapsulatedProtocolId : Attribute() {
    override val id = SSTP_ATTRIBUTE_ID_ENCAPSULATED_PROTOCOL_ID
    override val length = 6

    internal var protocolId: Short = 1 // 协议类型

    /**
     * 读取协议ID属性内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        assertAlways(givenLength == length)
        protocolId = buffer.short
    }

    /**
     * 写入协议ID属性内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.putShort(protocolId)
    }
}

/**
 * 状态信息属性
 * 传递连接状态及附加数据
 */
internal class StatusInfo : Attribute() {
    override val id = SSTP_ATTRIBUTE_ID_STATUS_INFO

    override val length: Int
        get() = minimumLength + holder.size

    private val minimumLength = 12
    private val maximumHolderSize = 64

    internal var targetId: Byte = 0 // 目标属性ID
    internal var status: Int = 0 // 状态码
    internal var holder = ByteArray(0) // 附加数据

    /**
     * 读取状态信息属性内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        val holderSize = givenLength - minimumLength
        assertAlways(holderSize in 0..maximumHolderSize)
        buffer.move(3)
        targetId = buffer.get()
        status = buffer.int
        holder = ByteArray(holderSize).also {
            buffer.get(it)
        }
    }

    /**
     * 写入状态信息属性内容
     */
    override fun write(buffer: ByteBuffer) {
        assertAlways(holder.size <= maximumHolderSize)
        writeHeader(buffer)
        buffer.padZeroByte(3)
        buffer.put(targetId)
        buffer.putInt(status)
        buffer.put(holder)
    }
}

/**
 * 加密绑定属性
 * 包含哈希协议、随机数、证书哈希、MAC等
 */
internal class CryptoBinding : Attribute() {
    override val id = SSTP_ATTRIBUTE_ID_CRYPTO_BINDING
    override val length = 104

    internal var hashProtocol: Byte = 2 // 哈希协议类型
    internal val nonce = ByteArray(32) // 随机数
    internal val certHash = ByteArray(32) // 证书哈希
    internal val compoundMac = ByteArray(32) // MAC值

    /**
     * 读取加密绑定属性内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        assertAlways(givenLength == length)
        buffer.move(3)
        hashProtocol = buffer.get()
        buffer.get(nonce)
        buffer.get(certHash)
        buffer.get(compoundMac)
    }

    /**
     * 写入加密绑定属性内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.padZeroByte(3)
        buffer.put(hashProtocol)
        buffer.put(nonce)
        buffer.put(certHash)
        buffer.put(compoundMac)
    }
}

/**
 * 加密绑定请求属性
 * 包含请求标志和随机数
 */
internal class CryptoBindingRequest : Attribute() {
    override val id = SSTP_ATTRIBUTE_ID_CRYPTO_BINDING_REQ
    override val length = 40

    internal var bitmask: Byte = 3 // 请求标志
    internal val nonce = ByteArray(32) // 随机数

    /**
     * 读取加密绑定请求属性内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        assertAlways(givenLength == length)
        buffer.move(3)
        bitmask = buffer.get()
        buffer.get(nonce)
    }

    /**
     * 写入加密绑定请求属性内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.padZeroByte(3)
        buffer.put(bitmask)
        buffer.put(nonce)
    }
}