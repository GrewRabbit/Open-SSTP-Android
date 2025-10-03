/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/option/ipcp.kt
 * 作用说明：
 * 本文件实现了PPP协议中IPCP（IP Control Protocol）相关的选项结构，包括IP地址和DNS地址的协商选项。
 * 主要功能：
 * - IpcpAddressOption：用于表示和处理IPCP的IP地址或DNS地址选项，支持读写操作。
 * - IpcpOptionPack：用于管理和解析IPCP选项集合，自动识别IP和DNS选项。
 * 在本项目中，PPP链路协商流程的相关模块（如PPP帧解析器、IPCP协商控制器）会调用本文件，
 * 用于解析和构建IPCP选项数据，实现IP地址和DNS地址的协商。
 * 本文件依赖Option和OptionPack基类（统一选项读写接口），以及字节处理工具方法（probeByte）。
 * 其它PPP协议相关的选项处理文件也会与本文件协作，实现完整的选项协商与数据收发。
 */

package kittoku.osc.unit.ppp.option

import kittoku.osc.extension.probeByte
import java.nio.ByteBuffer

// IPCP选项类型常量
internal const val OPTION_TYPE_IPCP_IP: Byte = 0x03      // IP地址选项
internal const val OPTION_TYPE_IPCP_DNS = 0x81.toByte() // DNS地址选项

/**
 * IPCP地址选项（IP或DNS）
 * 负责读写IPCP的IP地址或DNS地址选项内容
 */
internal class IpcpAddressOption(override val type: Byte) : Option() {
    internal val address = ByteArray(4) // 4字节地址
    override val length = headerSize + address.size

    /**
     * 从ByteBuffer读取地址选项内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        buffer.get(address)
    }

    /**
     * 将地址选项内容写入ByteBuffer
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.put(address)
    }
}

/**
 * IPCP选项集合
 * 管理IP和DNS选项，负责解析和收集所有IPCP选项
 */
internal class IpcpOptionPack(givenLength: Int = 0) : OptionPack(givenLength) {
    internal var ipOption: IpcpAddressOption? = null  // IP地址选项
    internal var dnsOption: IpcpAddressOption? = null // DNS地址选项

    /**
     * 返回已识别的所有IPCP选项
     */
    override val knownOptions: List<Option>
        get() = mutableListOf<Option>().also { options ->
            ipOption?.also { options.add(it) }
            dnsOption?.also { options.add(it) }
        }

    /**
     * 解析并返回一个IPCP选项，根据类型自动识别IP或DNS
     */
    override fun retrieveOption(buffer: ByteBuffer): Option {
        val option = when (val type = buffer.probeByte(0)) {
            OPTION_TYPE_IPCP_IP -> IpcpAddressOption(type).also { ipOption = it }
            OPTION_TYPE_IPCP_DNS -> IpcpAddressOption(type).also { dnsOption = it }
            else -> UnknownOption(type)
        }
        option.read(buffer)
        return option
    }
}