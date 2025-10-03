/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/auth/EAPFrame.kt
 * 作用说明：
 * 本文件定义了PPP协议EAP（可扩展认证协议）认证相关的数据帧结构，包括Request、Response、Success、Failure等帧类型。
 * 主要功能：
 * - EAPFrame：EAP帧的抽象基类，指定协议类型。
 * - EAPDataFrame：带有Type和TypeData字段的EAP帧抽象类，供Request和Response帧继承。
 * - EAPRequest/EAPResponse：具体EAP数据帧类型，分别对应认证流程中的请求和响应。
 * - EAPResultFrame：无TypeData的EAP结果帧抽象类，供Success和Failure帧继承。
 * - EAPSuccess/EAPFailure：具体EAP结果帧类型，分别对应认证成功和失败。
 * 在本项目中，PPP认证流程的EAP处理模块会调用本文件，用于解析和构建EAP认证数据帧。
 * 本文件会调用`Frame`基类（统一PPP帧读写接口）、`PPP_PROTOCOL_EAP`协议常量。
 * 其它PPP协议相关的认证与数据帧处理文件也会与本文件协作，实现完整的认证数据收发。
 */

package kittoku.osc.unit.ppp.auth

import kittoku.osc.unit.ppp.Frame
import kittoku.osc.unit.ppp.PPP_PROTOCOL_EAP
import java.nio.ByteBuffer

// EAP协议的各类Code常量
internal const val EAP_CODE_REQUEST: Byte = 1
internal const val EAP_CODE_RESPONSE: Byte = 2
internal const val EAP_CODE_SUCCESS: Byte = 3
internal const val EAP_CODE_FAILURE: Byte = 4

// EAP协议的Type常量
internal const val EAP_TYPE_IDENTITY: Byte = 1
internal const val EAP_TYPE_NOTIFICATION: Byte = 2
internal const val EAP_TYPE_NAK: Byte = 3
internal const val EAP_TYPE_MS_AUTH: Byte = 26 // MSCHAPV2

/**
 * EAP帧抽象基类，指定PPP协议类型
 */
internal abstract class EAPFrame : Frame() {
    override val protocol = PPP_PROTOCOL_EAP
}

/**
 * 带有Type和TypeData字段的EAP帧抽象类
 * 供Request和Response帧继承
 */
internal abstract class EAPDataFrame : EAPFrame() {
    override val length: Int
        get() = headerSize + 1 + typeData.size

    internal var type: Byte = 0 // EAP类型字段
    internal var typeData = ByteArray(0) // EAP类型数据字段

    /**
     * 读取EAP数据帧（含Type和TypeData字段）
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        type = buffer.get()
        typeData = ByteArray(givenLength - length)
        buffer.get(typeData)
    }

    /**
     * 写入EAP数据帧（含Type和TypeData字段）
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.put(type)
        buffer.put(typeData)
    }
}

/**
 * EAP Request帧
 * 用于服务端发起认证请求
 */
internal class EAPRequest : EAPDataFrame() {
    override val code = EAP_CODE_REQUEST
}

/**
 * EAP Response帧
 * 用于客户端响应认证请求
 */
internal class EAPResponse : EAPDataFrame() {
    override val code = EAP_CODE_RESPONSE
}

/**
 * 无TypeData的EAP结果帧抽象类
 * 供Success和Failure帧继承
 */
internal abstract class EAPResultFrame : EAPFrame() {
    override val length = headerSize

    /**
     * 读取EAP结果帧（仅头部）
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
    }

    /**
     * 写入EAP结果帧（仅头部）
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
    }
}

/**
 * EAP Success帧
 * 用于认证成功时服务端返回
 */
internal class EAPSuccess : EAPResultFrame() {
    override val code = EAP_CODE_SUCCESS
}

/**
 * EAP Failure帧
 * 用于认证失败时服务端返回
 */
internal class EAPFailure : EAPResultFrame() {
    override val code = EAP_CODE_FAILURE
}