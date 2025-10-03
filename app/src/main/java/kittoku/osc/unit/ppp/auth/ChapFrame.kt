/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/auth/ChapFrame.kt
 * 作用说明：
 * 本文件定义了PPP协议CHAP认证相关的数据帧结构，包括Challenge、Response、Success、Failure等帧类型。
 * 主要功能：
 * - ChapFrame：CHAP帧的抽象基类，指定协议类型。
 * - ChapValueNameFrame：带有Value/Name字段的CHAP帧抽象类，供Challenge和Response帧继承。
 * - ChapMessageFrame：带有Message字段的CHAP帧抽象类，供Success和Failure帧继承。
 * - ChapChallenge/ChapResponse/ChapSuccess/ChapFailure：具体CHAP帧类型，分别对应认证流程中的挑战、响应、成功、失败。
 * 在本项目中，PPP认证流程的CHAP处理模块会调用本文件，用于解析和构建CHAP认证数据帧。
 * 本文件会调用`ChapValueNameFiled`和`ChapMessageField`（定义于同目录下），用于具体字段的读写操作。
 * 其它PPP协议相关的认证与数据帧处理文件也会与本文件协作，实现完整的认证数据收发。
 */

package kittoku.osc.unit.ppp.auth

import kittoku.osc.unit.ppp.Frame
import kittoku.osc.unit.ppp.PPP_PROTOCOL_CHAP
import java.nio.ByteBuffer

// CHAP协议的各类Code常量
internal const val CHAP_CODE_CHALLENGE: Byte = 1
internal const val CHAP_CODE_RESPONSE: Byte = 2
internal const val CHAP_CODE_SUCCESS: Byte = 3
internal const val CHAP_CODE_FAILURE: Byte = 4

/**
 * CHAP帧抽象基类，指定PPP协议类型
 */
internal abstract class ChapFrame : Frame() {
    override val protocol = PPP_PROTOCOL_CHAP
}

/**
 * 带有Value/Name字段的CHAP帧抽象类
 * 供Challenge和Response帧继承
 */
internal abstract class ChapValueNameFrame : ChapFrame() {
    internal var valueName = ChapValueNameFiled() // Value/Name字段单元
    override val length: Int
        get() = headerSize + valueName.length

    /**
     * 读取CHAP帧（含Value/Name字段）
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        valueName.givenLength = givenLength - headerSize
        valueName.read(buffer)
    }

    /**
     * 写入CHAP帧（含Value/Name字段）
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        valueName.write(buffer)
    }
}

/**
 * CHAP Challenge帧
 * 用于服务端发起认证挑战
 */
internal class ChapChallenge : ChapValueNameFrame() {
    override val code = CHAP_CODE_CHALLENGE
}

/**
 * CHAP Response帧
 * 用于客户端响应认证挑战
 */
internal class ChapResponse : ChapValueNameFrame() {
    override val code = CHAP_CODE_RESPONSE
}

/**
 * 带有Message字段的CHAP帧抽象类
 * 供Success和Failure帧继承
 */
internal abstract class ChapMessageFrame : ChapFrame() {
    internal var message = ChapMessageField() // Message字段单元
    override val length: Int
        get() = headerSize + message.length

    /**
     * 读取CHAP帧（含Message字段）
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        message.givenLength = givenLength - headerSize
        message.read(buffer)
    }

    /**
     * 写入CHAP帧（含Message字段）
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        message.write(buffer)
    }
}

/**
 * CHAP Success帧
 * 用于认证成功时服务端返回
 */
internal class ChapSuccess : ChapMessageFrame() {
    override val code = CHAP_CODE_SUCCESS
}

/**
 * CHAP Failure帧
 * 用于认证失败时服务端返回
 */
internal class ChapFailure : ChapMessageFrame() {
    override val code = CHAP_CODE_FAILURE
}