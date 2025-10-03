/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/Ipv6cpFrame.kt
 * 作用说明：
 * 本文件实现了PPP协议中IPv6CP（IPv6 Control Protocol）协商相关的数据帧结构，包括请求、确认、否定和拒绝等帧类型。
 * 主要功能：
 * - Ipv6cpFrame：IPv6CP帧抽象基类，统一协议类型。
 * - Ipv6cpConfigureFrame：IPv6CP配置帧抽象基类，管理IPv6CP选项集合的读写。
 * - Ipv6cpConfigureRequest/Ack/Nak/Reject：具体的IPv6CP协商帧类型，分别对应请求、确认、否定和拒绝。
 * 在本项目中，PPP链路协商流程的相关模块（如PPP帧解析器、IPv6CP协商控制器）会调用本文件，
 * 用于解析和构建IPv6CP协商帧，实现IPv6链路标识符的协商。
 * 本文件会调用Frame基类（统一PPP帧结构）、Ipv6cpOptionPack（IPv6CP选项集合的读写）。
 * 其它PPP协议相关的帧处理文件（如LCPFrame、IpcpFrame等）会与本文件协作，实现完整的链路协商流程。
 */

package kittoku.osc.unit.ppp

import kittoku.osc.unit.ppp.option.Ipv6cpOptionPack
import java.nio.ByteBuffer

/**
 * IPv6CP帧抽象基类
 * 统一IPv6CP协议类型
 */
internal abstract class Ipv6cpFrame : Frame() {
    override val protocol = PPP_PROTOCOL_IPv6CP
}

/**
 * IPv6CP配置帧抽象基类
 * 管理IPv6CP选项集合的读写
 */
internal abstract class Ipv6cpConfigureFrame : Ipv6cpFrame() {
    override val length: Int
        get() = headerSize + options.length

    internal var options: Ipv6cpOptionPack = Ipv6cpOptionPack()

    /**
     * 读取IPv6CP配置帧内容，包括头部和选项集合
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        options = Ipv6cpOptionPack(givenLength - length).also {
            it.read(buffer)
        }
    }

    /**
     * 写入IPv6CP配置帧内容，包括头部和选项集合
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        options.write(buffer)
    }
}

/**
 * IPv6CP配置请求帧
 * 用于发起IPv6CP协商请求
 */
internal class Ipv6cpConfigureRequest : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REQUEST
}

/**
 * IPv6CP配置确认帧
 * 用于确认对方的IPv6CP协商请求
 */
internal class Ipv6cpConfigureAck : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_ACK
}

/**
 * IPv6CP配置否定帧
 * 用于否定对方的IPv6CP协商请求，建议修改
 */
internal class Ipv6cpConfigureNak : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_NAK
}

/**
 * IPv6CP配置拒绝帧
 * 用于拒绝对方的IPv6CP协商请求
 */
internal class Ipv6cpConfigureReject : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REJECT
}