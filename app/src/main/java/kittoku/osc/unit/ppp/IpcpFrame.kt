/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/IpcpFrame.kt
 * 作用说明：
 * 本文件实现了PPP协议中IPCP（IP Control Protocol）协商相关的数据帧结构，包括请求、确认、拒绝等帧类型。
 * 主要功能：
 * - IpcpFrame：IPCP帧抽象基类，统一协议类型。
 * - IpcpConfigureFrame：IPCP配置帧抽象基类，管理IPCP选项集合的读写。
 * - IpcpConfigureRequest/Ack/Nak/Reject：具体的IPCP协商帧类型，分别对应请求、确认、否定和拒绝。
 * 在本项目中，PPP链路协商流程的相关模块（如PPP帧解析器、IPCP协商控制器）会调用本文件，
 * 用于解析和构建IPCP协商帧，实现IP地址和DNS地址的协商。
 * 本文件会调用Frame基类（统一PPP帧结构）、IpcpOptionPack（IPCP选项集合的读写）。
 * 其它PPP协议相关的帧处理文件（如LCPFrame、IPv6cpFrame等）会与本文件协作，实现完整的链路协商流程。
 */

package kittoku.osc.unit.ppp

import kittoku.osc.unit.ppp.option.IpcpOptionPack
import java.nio.ByteBuffer

/**
 * IPCP帧抽象基类
 * 统一IPCP协议类型
 */
internal abstract class IpcpFrame : Frame() {
    override val protocol = PPP_PROTOCOL_IPCP
}

/**
 * IPCP配置帧抽象基类
 * 管理IPCP选项集合的读写
 */
internal abstract class IpcpConfigureFrame : IpcpFrame() {
    override val length: Int
        get() = headerSize + options.length

    internal var options: IpcpOptionPack = IpcpOptionPack()

    /**
     * 读取IPCP配置帧内容，包括头部和选项集合
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        options = IpcpOptionPack(givenLength - length).also {
            it.read(buffer)
        }
    }

    /**
     * 写入IPCP配置帧内容，包括头部和选项集合
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        options.write(buffer)
    }
}

/**
 * IPCP配置请求帧
 * 用于发起IPCP协商请求
 */
internal class IpcpConfigureRequest : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REQUEST
}

/**
 * IPCP配置确认帧
 * 用于确认对方的IPCP协商请求
 */
internal class IpcpConfigureAck : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_ACK
}

/**
 * IPCP配置否定帧
 * 用于否定对方的IPCP协商请求，建议修改
 */
internal class IpcpConfigureNak : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_NAK
}

/**
 * IPCP配置拒绝帧
 * 用于拒绝对方的IPCP协商请求
 */
internal class IpcpConfigureReject : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REJECT
}