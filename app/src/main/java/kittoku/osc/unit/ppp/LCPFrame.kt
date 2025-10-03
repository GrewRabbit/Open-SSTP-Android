/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/LCPFrame.kt
 * 作用说明：
 * 本文件实现了PPP协议中LCP（Link Control Protocol）相关的数据帧结构，包括配置协商、终止、拒绝、回显等帧类型。
 * 主要功能：
 * - LCPFrame：LCP帧抽象基类，统一协议类型。
 * - LCPConfigureFrame及其子类：管理LCP选项集合，实现协商请求、确认、否定、拒绝等帧。
 * - LCPDataHoldingFrame及其子类：处理带数据的LCP帧，如终止、代码拒绝等。
 * - LCPProtocolReject：处理协议拒绝帧，包含被拒绝协议类型。
 * - LCPMagicNumberFrame及其子类：处理带魔术数的LCP帧，如回显请求/应答、丢弃请求。
 * 在本项目中，PPP链路协商流程的相关模块（如PPP帧解析器、LCP协商控制器）会调用本文件，
 * 用于解析和构建LCP相关的协商与控制帧，实现链路参数、认证方式、链路检测等功能。
 * 本文件会调用Frame基类（统一PPP帧结构）、LCPOptionPack（LCP选项集合的读写）、字节处理工具方法（assertAlways）。
 * 其它PPP协议相关的帧处理文件（如IpcpFrame、Ipv6cpFrame等）会与本文件协作，实现完整的链路协商流程。
 */

package kittoku.osc.unit.ppp

import kittoku.osc.debug.assertAlways
import kittoku.osc.unit.ppp.option.LCPOptionPack
import java.nio.ByteBuffer

// LCP帧类型码常量定义
internal const val LCP_CODE_CONFIGURE_REQUEST: Byte = 1      // 配置请求
internal const val LCP_CODE_CONFIGURE_ACK: Byte = 2          // 配置确认
internal const val LCP_CODE_CONFIGURE_NAK: Byte = 3          // 配置否定
internal const val LCP_CODE_CONFIGURE_REJECT: Byte = 4       // 配置拒绝
internal const val LCP_CODE_TERMINATE_REQUEST: Byte = 5      // 终止请求
internal const val LCP_CODE_TERMINATE_ACK: Byte = 6          // 终止确认
internal const val LCP_CODE_CODE_REJECT: Byte = 7            // 代码拒绝
internal const val LCP_CODE_PROTOCOL_REJECT: Byte = 8        // 协议拒绝
internal const val LCP_CODE_ECHO_REQUEST: Byte = 9           // 回显请求
internal const val LCP_CODE_ECHO_REPLY: Byte = 10            // 回显应答
internal const val LCP_CODE_DISCARD_REQUEST: Byte = 11       // 丢弃请求

/**
 * LCP帧抽象基类
 * 统一LCP协议类型
 */
internal abstract class LCPFrame : Frame() {
    override val protocol = PPP_PROTOCOL_LCP
}

/**
 * LCP配置帧抽象基类
 * 管理LCP选项集合的读写
 */
internal abstract class LCPConfigureFrame : LCPFrame() {
    override val length: Int
        get() = headerSize + options.length

    internal var options: LCPOptionPack = LCPOptionPack()

    /**
     * 读取LCP配置帧内容，包括头部和选项集合
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        options = LCPOptionPack(givenLength - length).also {
            it.read(buffer)
        }
    }

    /**
     * 写入LCP配置帧内容，包括头部和选项集合
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        options.write(buffer)
    }
}

/**
 * LCP配置请求帧
 * 用于发起LCP协商请求
 */
internal class LCPConfigureRequest : LCPConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REQUEST
}

/**
 * LCP配置确认帧
 * 用于确认对方的LCP协商请求
 */
internal class LCPConfigureAck : LCPConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_ACK
}

/**
 * LCP配置否定帧
 * 用于否定对方的LCP协商请求，建议修改
 */
internal class LCPConfigureNak : LCPConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_NAK
}

/**
 * LCP配置拒绝帧
 * 用于拒绝对方的LCP协商请求
 */
internal class LCPConfigureReject : LCPConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REJECT
}

/**
 * LCP带数据帧抽象类
 * 处理如终止、代码拒绝等带数据的LCP帧
 */
internal abstract class LCPDataHoldingFrame : LCPFrame() {
    override val length: Int
        get() = headerSize + holder.size

    internal var holder = ByteArray(0)

    /**
     * 读取LCP数据帧内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        val holderSize = givenLength - length
        assertAlways(holderSize >= 0)
        if (holderSize > 0) {
            holder = ByteArray(holderSize).also { buffer.get(it) }
        }
    }

    /**
     * 写入LCP数据帧内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.put(holder)
    }
}

/**
 * LCP终止请求帧
 * 用于发起链路终止请求
 */
internal class LCPTerminalRequest : LCPDataHoldingFrame() {
    override val code = LCP_CODE_TERMINATE_REQUEST
}

/**
 * LCP终止确认帧
 * 用于确认链路终止
 */
internal class LCPTerminalAck : LCPDataHoldingFrame() {
    override val code = LCP_CODE_TERMINATE_ACK
}

/**
 * LCP代码拒绝帧
 * 用于拒绝无法识别的代码类型
 */
internal class LCPCodeReject : LCPDataHoldingFrame() {
    override val code = LCP_CODE_CODE_REJECT
}

/**
 * LCP协议拒绝帧
 * 用于拒绝无法识别的协议类型
 */
internal class LCPProtocolReject : LCPFrame() {
    override val code = LCP_CODE_PROTOCOL_REJECT
    override val length: Int
        get() = headerSize + Short.SIZE_BYTES + holder.size

    internal var rejectedProtocol: Short = 0
    internal var holder = ByteArray(0)

    /**
     * 读取协议拒绝帧内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        rejectedProtocol = buffer.short
        val holderSize = givenLength - length
        assertAlways(holderSize >= 0)
        if (holderSize > 0) {
            holder = ByteArray(holderSize).also { buffer.get(it) }
        }
    }

    /**
     * 写入协议拒绝帧内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.putShort(rejectedProtocol)
        buffer.put(holder)
    }
}

/**
 * LCP带魔术数帧抽象类
 * 处理如回显请求/应答、丢弃请求等带魔术数的LCP帧
 */
internal abstract class LCPMagicNumberFrame : LCPFrame() {
    override val length: Int
        get() = headerSize + Int.SIZE_BYTES + holder.size

    internal var magicNumber = 0
    internal var holder = ByteArray(0)

    /**
     * 读取魔术数帧内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        magicNumber = buffer.int
        val holderSize = givenLength - length
        assertAlways(holderSize >= 0)
        if (holderSize > 0) {
            holder = ByteArray(holderSize).also { buffer.get(it) }
        }
    }

    /**
     * 写入魔术数帧内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.putInt(magicNumber)
        buffer.put(holder)
    }
}

/**
 * LCP回显请求帧
 * 用于链路检测
 */
internal class LCPEchoRequest : LCPMagicNumberFrame() {
    override val code = LCP_CODE_ECHO_REQUEST
}

/**
 * LCP回显应答帧
 * 用于响应链路检测
 */
internal class LCPEchoReply : LCPMagicNumberFrame() {
    override val code = LCP_CODE_ECHO_REPLY
}

/**
 * LCP丢弃请求帧
 * 用于测试链路丢弃能力
 */
internal class LcpDiscardRequest : LCPMagicNumberFrame() {
    override val code = LCP_CODE_DISCARD_REQUEST
}