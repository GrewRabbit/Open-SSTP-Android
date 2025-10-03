/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/auth/PAPFrame.kt
 * 作用说明：
 * 本文件定义了PPP协议PAP（Password Authentication Protocol）认证相关的数据帧结构，包括认证请求、认证成功、认证失败等帧类型。
 * 主要功能：
 * - PAPFrame：PAP帧的抽象基类，指定协议类型。
 * - PAPAuthenticateRequest：PAP认证请求帧，包含用户名和密码字段，客户端用于发起认证。
 * - PAPAuthenticateAcknowledgement：PAP认证结果帧抽象类，包含可选的消息字段，供Ack和Nak继承。
 * - PAPAuthenticateAck：认证成功帧，服务端返回。
 * - PAPAuthenticateNak：认证失败帧，服务端返回。
 * 在本项目中，PPP认证流程的PAP处理模块会调用本文件，用于解析和构建PAP认证数据帧。
 * 本文件依赖Frame基类（统一PPP帧读写接口）、PPP协议常量、字节处理工具方法等。
 * 其它PPP协议相关的认证与数据帧处理文件也会与本文件协作，实现完整的认证数据收发。
 */

package kittoku.osc.unit.ppp.auth

import kittoku.osc.debug.ParsingDataUnitException
import kittoku.osc.debug.assertAlways
import kittoku.osc.extension.toIntAsUByte
import kittoku.osc.unit.ppp.Frame
import kittoku.osc.unit.ppp.PPP_PROTOCOL_PAP
import java.nio.ByteBuffer

// PAP协议的各类Code常量
internal const val PAP_CODE_AUTHENTICATE_REQUEST: Byte = 1
internal const val PAP_CODE_AUTHENTICATE_ACK: Byte = 2
internal const val PAP_CODE_AUTHENTICATE_NAK: Byte = 3

/**
 * PAP帧抽象基类，指定PPP协议类型
 */
internal abstract class PAPFrame : Frame() {
    override val protocol = PPP_PROTOCOL_PAP
}

/**
 * PAP认证请求帧
 * 包含用户名和密码字段，客户端用于发起认证
 */
internal class PAPAuthenticateRequest : PAPFrame() {
    override val code = PAP_CODE_AUTHENTICATE_REQUEST
    override val length: Int
        get() = headerSize + 1 + idFiled.size + 1 + passwordFiled.size

    internal var idFiled = ByteArray(0)       // PAP用户名字段
    internal var passwordFiled = ByteArray(0) // PAP密码字段

    /**
     * 读取认证请求帧（用户名和密码）
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)

        val idLength = buffer.get().toIntAsUByte()
        idFiled = ByteArray(idLength).also { buffer.get(it) }

        val passwordLength = buffer.get().toIntAsUByte()
        passwordFiled = ByteArray(passwordLength).also { buffer.get(it) }
    }

    /**
     * 写入认证请求帧（用户名和密码）
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        buffer.put(idFiled.size.toByte())
        buffer.put(idFiled)
        buffer.put(passwordFiled.size.toByte())
        buffer.put(passwordFiled)
    }
}

/**
 * PAP认证结果帧抽象类
 * 包含可选的消息字段，供Ack和Nak继承
 */
internal abstract class PAPAuthenticateAcknowledgement : PAPFrame() {
    override val length: Int
        get() = headerSize + (if (message.isEmpty()) 0 else message.size + 1)

    private var message = ByteArray(0) // PAP认证结果消息字段

    /**
     * 读取认证结果帧（可选消息）
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)

        when (val remaining = length - headerSize) {
            0 -> {} // 无消息字段
            in 1..Int.MAX_VALUE -> {
                val messageLength = buffer.get().toIntAsUByte()
                assertAlways(messageLength == remaining - 1)
                message = ByteArray(messageLength).also { buffer.get(it) }
            }
            else -> throw ParsingDataUnitException()
        }
    }

    /**
     * 写入认证结果帧（可选消息）
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        if (message.isNotEmpty()) {
            buffer.put(message.size.toByte())
            buffer.put(message)
        }
    }
}

/**
 * PAP认证成功帧
 * 服务端返回
 */
internal class PAPAuthenticateAck : PAPAuthenticateAcknowledgement() {
    override val code = PAP_CODE_AUTHENTICATE_ACK
}

/**
 * PAP认证失败帧
 * 服务端返回
 */
internal class PAPAuthenticateNak : PAPAuthenticateAcknowledgement() {
    override val code = PAP_CODE_AUTHENTICATE_NAK
}