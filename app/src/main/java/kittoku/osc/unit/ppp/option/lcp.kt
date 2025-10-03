/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/option/lcp.kt
 * 作用说明：
 * 本文件实现了PPP协议中LCP（Link Control Protocol）相关的选项结构，包括MRU（最大接收单元）和认证协议选项。
 * 主要功能：
 * - MRUOption：用于表示和处理LCP的MRU选项，支持读写操作。
 * - AuthOption：用于表示和处理LCP的认证协议选项，支持读写操作。
 * - LCPOptionPack：用于管理和解析LCP选项集合，自动识别MRU和认证选项。
 * 在本项目中，PPP链路协商流程的相关模块（如PPP帧解析器、LCP协商控制器）会调用本文件，
 * 用于解析和构建LCP选项数据，实现链路参数和认证方式的协商。
 * 本文件依赖Option和OptionPack基类（统一选项读写接口），以及字节处理工具方法（probeByte、toIntAsUShort）。
 * 其它PPP协议相关的选项处理文件也会与本文件协作，实现完整的选项协商与数据收发。
 */

package kittoku.osc.unit.ppp.option

import kittoku.osc.debug.assertAlways
import kittoku.osc.extension.probeByte
import kittoku.osc.extension.toIntAsUShort
import java.nio.ByteBuffer

// LCP选项类型常量
internal const val OPTION_TYPE_LCP_MRU: Byte = 1      // MRU选项
internal const val OPTION_TYPE_LCP_AUTH: Byte = 3     // 认证协议选项

// CHAP算法常量
internal const val CHAP_ALGORITHM_MSCHAPv2 = 0x81.toByte()

/**
 * MRU（Maximum Receive Unit）选项
 * 负责读写LCP的MRU选项内容
 */
internal class MRUOption : Option() {
    override val type = OPTION_TYPE_LCP_MRU
    override val length = headerSize + Short.SIZE_BYTES

    internal var unitSize = 0 // 最大接收单元长度

    /**
     * 从ByteBuffer读取MRU选项内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        unitSize = buffer.short.toIntAsUShort()
    }

    /**
     * 将MRU选项内容写入ByteBuffer
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.putShort(unitSize.toShort())
    }
}

/**
 * 认证协议选项
 * 负责读写LCP的认证协议选项内容
 */
internal class AuthOption : Option() {
    override val type = OPTION_TYPE_LCP_AUTH
    internal var protocol: Short = 0 // 认证协议类型
    internal var holder = ByteArray(0) // 认证协议附加字段
    override val length: Int
        get() = headerSize + Short.SIZE_BYTES + holder.size

    /**
     * 从ByteBuffer读取认证协议选项内容
     */
    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)
        protocol = buffer.getShort()
        val holderSize = givenLength - length
        assertAlways(holderSize >= 0)
        if (holderSize > 0) {
            holder = ByteArray(holderSize).also { buffer.get(it) }
        }
    }

    /**
     * 将认证协议选项内容写入ByteBuffer
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)
        buffer.putShort(protocol)
        buffer.put(holder)
    }
}

/**
 * LCP选项集合
 * 管理MRU和认证协议选项，负责解析和收集所有LCP选项
 */
internal class LCPOptionPack(givenLength: Int = 0) : OptionPack(givenLength) {
    internal var mruOption: MRUOption? = null      // MRU选项
    internal var authOption: AuthOption? = null    // 认证协议选项

    /**
     * 返回已识别的所有LCP选项
     */
    override val knownOptions: List<Option>
        get() = mutableListOf<Option>().also { options ->
            mruOption?.also { options.add(it) }
            authOption?.also { options.add(it) }
        }

    /**
     * 解析并返回一个LCP选项，根据类型自动识别MRU或认证协议
     */
    override fun retrieveOption(buffer: ByteBuffer): Option {
        val option = when (val type = buffer.probeByte(0)) {
            OPTION_TYPE_LCP_MRU -> MRUOption().also { mruOption = it }
            OPTION_TYPE_LCP_AUTH -> AuthOption().also { authOption = it }
            else -> UnknownOption(type)
        }
        option.read(buffer)
        return option
    }
}