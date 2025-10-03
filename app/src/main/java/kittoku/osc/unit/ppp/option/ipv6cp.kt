/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/option/ipv6cp.kt
 * 作用说明：
 * 本文件实现了PPP协议中IPv6CP（IPv6 Control Protocol）相关的选项结构，主要用于协商IPv6链路的标识符选项。
 * 主要功能：
 * - Ipv6cpIdentifierOption：用于表示和处理IPv6CP的Interface-Identifier选项，支持读写操作。
 * - Ipv6cpOptionPack：用于管理和解析IPv6CP选项集合，自动识别Identifier选项。
 * 在本项目中，PPP链路协商流程的相关模块（如PPP帧解析器、IPv6CP协商控制器）会调用本文件，
 * 用于解析和构建IPv6CP选项数据，实现IPv6链路标识符的协商。
 * 本文件依赖Option和OptionPack基类（统一选项读写接口），以及字节处理工具方法（probeByte）。
 * 其它PPP协议相关的选项处理文件也会与本文件协作，实现完整的选项协商与数据收发。
 */

package kittoku.osc.unit.ppp.option

import kittoku.osc.extension.probeByte
import java.nio.ByteBuffer

// IPv6CP选项类型常量
internal const val OPTION_TYPE_IPv6CP_IDENTIFIER: Byte = 0x01

/**
 * IPv6CP Identifier选项
 * 负责读写IPv6CP的Interface-Identifier选项内容
 */
internal class Ipv6cpIdentifierOption : Option() {
    override val type = OPTION_TYPE_IPv6CP_IDENTIFIER
    internal val identifier = ByteArray(8) // 8字节标识符
    override val length = headerSize + identifier.size

    /**
     * 从ByteBuffer读取Identifier选项内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        buffer.get(identifier)
    }

    /**
     * 将Identifier选项内容写入ByteBuffer
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.put(identifier)
    }
}

/**
 * IPv6CP选项集合
 * 管理Identifier选项，负责解析和收集所有IPv6CP选项
 */
internal class Ipv6cpOptionPack(givenLength: Int = 0) : OptionPack(givenLength) {
    internal var identifierOption: Ipv6cpIdentifierOption? = null // 标识符选项

    /**
     * 返回已识别的所有IPv6CP选项
     */
    override val knownOptions: List<Option>
        get() = mutableListOf<Option>().also { options ->
            identifierOption?.also { options.add(it) }
        }

    /**
     * 解析并返回一个IPv6CP选项，根据类型自动识别Identifier
     */
    override fun retrieveOption(buffer: ByteBuffer): Option {
        val option = when (val type = buffer.probeByte(0)) {
            OPTION_TYPE_IPv6CP_IDENTIFIER -> Ipv6cpIdentifierOption().also { identifierOption = it }
            else -> UnknownOption(type)
        }
        option.read(buffer)
        return option
    }
}