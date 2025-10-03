/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/auth/ChapField.kt
 * 作用说明：
 * 本文件定义了PPP协议CHAP认证相关的数据字段单元，包括`ChapValueNameFiled`和`ChapMessageField`。
 * 主要功能：
 * - ChapValueNameFiled：用于解析和构建CHAP认证中的Value/Name字段，支持读写操作。
 * - ChapMessageField：用于解析和构建CHAP认证中的Message字段，支持读写操作。
 * 在本项目中，PPP认证流程的相关模块（如CHAP认证处理器）会调用本文件，进行CHAP数据的解析与封装。
 * 本文件依赖`DataUnit`基类（用于统一数据单元读写接口），以及`kittoku.osc.extension.toIntAsUByte`工具方法。
 * 其它PPP协议相关的数据单元文件也会与本文件协作，实现完整的认证数据处理。
 */

package kittoku.osc.unit.ppp.auth

import kittoku.osc.extension.toIntAsUByte
import kittoku.osc.unit.DataUnit
import java.nio.ByteBuffer

/**
 * CHAP Value/Name字段单元
 * 负责解析和写入CHAP认证中的Value和Name部分
 */
internal class ChapValueNameFiled : DataUnit() {
    internal var value = ByteArray(0) // CHAP认证的Value部分
    internal var name = ByteArray(0)  // CHAP认证的Name部分
    internal var givenLength = 0 // 读取时需提前指定总长度
    override val length: Int
        get() = 1 + value.size + name.size // 1字节长度+value+name

    /**
     * 从ByteBuffer读取Value和Name字段
     */
    override fun read(buffer: ByteBuffer) {
        value = ByteArray(buffer.get().toIntAsUByte())
        buffer.get(value)

        name = ByteArray(givenLength - length)
        buffer.get(name)
    }

    /**
     * 将Value和Name字段写入ByteBuffer
     */
    override fun write(buffer: ByteBuffer) {
        buffer.put(value.size.toByte())
        buffer.put(value)
        buffer.put(name)
    }
}

/**
 * CHAP Message字段单元
 * 负责解析和写入CHAP认证中的Message部分
 */
internal class ChapMessageField : DataUnit() {
    internal var message = ByteArray(0) // CHAP认证的Message内容
    internal var givenLength = 0 // 读取时需提前指定长度
    override val length: Int
        get() = message.size

    /**
     * 从ByteBuffer读取Message字段
     */
    override fun read(buffer: ByteBuffer) {
        message = ByteArray(givenLength)
        buffer.get(message)
    }

    /**
     * 将Message字段写入ByteBuffer
     */
    override fun write(buffer: ByteBuffer) {
        buffer.put(message)
    }
}