/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/Frame.kt
 * 作用说明：
 * 本文件定义了PPP协议数据帧的抽象基类Frame及相关协议常量，负责PPP帧的头部读写和基本结构。
 * 主要功能：
 * - Frame：抽象类，统一PPP帧的读写接口，处理SSTP和PPP头部，管理帧ID、协议类型等。
 * - 定义PPP相关协议常量（LCP、PAP、CHAP、EAP、IPCP、IPv6CP、IP、IPv6等）。
 * 在本项目中，所有PPP帧相关的模块（如认证、选项协商、数据传输等）都会继承和调用本文件，
 * 用于实现具体协议帧的解析和构建（如PAPFrame、LCPFrame等）。
 * 本文件依赖DataUnit基类、字节处理工具方法（assertAlways、toIntAsUShort）、SSTP协议常量。
 * 其它PPP协议帧实现文件（如auth/PAPFrame.kt、option/lcp.kt等）会依赖本文件，实现具体协议帧的读写和管理。
 */

package kittoku.osc.unit.ppp

import kittoku.osc.debug.assertAlways
import kittoku.osc.extension.toIntAsUShort
import kittoku.osc.unit.DataUnit
import kittoku.osc.unit.sstp.SSTP_PACKET_TYPE_DATA
import java.nio.ByteBuffer

// PPP协议相关常量定义
internal const val PPP_HDLC_HEADER = 0xFF03.toShort()
internal const val PPP_HEADER_SIZE = 4 // code, id, and frame length

internal const val PPP_PROTOCOL_LCP = 0xC021.toShort()
internal const val PPP_PROTOCOL_PAP = 0xC023.toShort()
internal const val PPP_PROTOCOL_CHAP = 0xC223.toShort()
internal const val PPP_PROTOCOL_EAP = 0xC227.toShort()
internal const val PPP_PROTOCOL_IPCP = 0x8021.toShort()
internal const val PPP_PROTOCOL_IPv6CP = 0x8057.toShort()
internal const val PPP_PROTOCOL_IP = 0x0021.toShort()
internal const val PPP_PROTOCOL_IPv6 = 0x0057.toShort()

/**
 * PPP帧抽象基类
 * 统一PPP帧的读写接口，处理SSTP和PPP头部，管理帧ID、协议类型等
 */
internal abstract class Frame : DataUnit() {
    internal abstract val code: Byte // PPP帧类型码
    internal abstract val protocol: Short // PPP协议类型

    private val offsetSize = 8 // SSTP头到PPP HDLC头的偏移
    protected val headerSize = offsetSize + PPP_HEADER_SIZE

    protected var givenLength = 0 // 帧实际长度

    internal var id: Byte = 0 // PPP帧ID

    /**
     * 读取PPP帧头部
     * 包括SSTP头、PPP HDLC头、协议类型、帧类型码、ID、帧长度等
     */
    protected fun readHeader(buffer: ByteBuffer) {
        assertAlways(buffer.short == SSTP_PACKET_TYPE_DATA)
        givenLength = buffer.short.toIntAsUShort()

        assertAlways(buffer.short == PPP_HDLC_HEADER)
        assertAlways(buffer.short == protocol)
        assertAlways(buffer.get() == code)
        id = buffer.get()
        assertAlways(buffer.short.toIntAsUShort() + offsetSize == givenLength)
    }

    /**
     * 写入PPP帧头部
     * 按协议格式依次写入SSTP头、PPP HDLC头、协议类型、帧类型码、ID、帧长度等
     */
    protected fun writeHeader(buffer: ByteBuffer) {
        buffer.putShort(SSTP_PACKET_TYPE_DATA)
        buffer.putShort(length.toShort())

        buffer.putShort(PPP_HDLC_HEADER)
        buffer.putShort(protocol)
        buffer.put(code)
        buffer.put(id)
        buffer.putShort((length - offsetSize).toShort())
    }
}