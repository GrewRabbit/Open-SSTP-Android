/**
 * 文件：app/src/main/java/kittoku/osc/unit/ppp/option/Option.kt
 * 作用说明：
 * 本文件定义了PPP协议选项（Option）及选项集合（OptionPack）的基础结构和读写逻辑。
 * 主要功能：
 * - Option：抽象类，表示PPP选项的通用结构，负责选项头部的读写。
 * - UnknownOption：用于处理未识别类型的PPP选项，保证协议兼容性。
 * - OptionPack：抽象类，管理多个选项的集合，支持选项顺序和读写操作。
 * 在本项目中，所有PPP选项相关的模块（如LCP、IPCP、IPv6CP等选项处理器）会继承和调用本文件，
 * 用于实现具体选项的解析、构建和序列化。
 * 本文件会调用`DataUnit`基类、字节处理工具方法（如`assertAlways`、`toIntAsUByte`）。
 * 其它PPP选项实现文件（如`lcp.kt`、`ipcp.kt`、`ipv6cp.kt`）会依赖本文件，实现具体协议选项的读写和管理。
 */

package kittoku.osc.unit.ppp.option

import kittoku.osc.debug.assertAlways
import kittoku.osc.extension.toIntAsUByte
import kittoku.osc.unit.DataUnit
import java.nio.ByteBuffer

/**
 * PPP选项抽象基类，定义选项类型和头部读写逻辑
 */
internal abstract class Option : DataUnit() {
    internal abstract val type: Byte // 选项类型

    protected val headerSize = 2 // 选项头部长度
    protected var givenLength = 0 // 选项实际长度

    /**
     * 读取选项头部（类型和长度）
     */
    protected open fun readHeader(buffer: ByteBuffer) {
        assertAlways(type == buffer.get())
        givenLength = buffer.get().toIntAsUByte()
    }

    /**
     * 写入选项头部（类型和长度）
     */
    protected open fun writeHeader(buffer: ByteBuffer) {
        buffer.put(type)
        buffer.put(length.toByte())
    }
}

/**
 * 未识别类型的PPP选项
 * 用于兼容未知选项类型，保证协议扩展性
 */
internal class UnknownOption(unknownType: Byte) : Option() {
    override val type = unknownType
    override val length: Int
        get() = headerSize + holder.size

    internal var holder = ByteArray(0) // 未知选项内容

    /**
     * 读取未知选项内容
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
     * 写入未知选项内容
     */
    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        buffer.put(holder)
    }
}

/**
 * PPP选项集合抽象类
 * 管理多个选项，支持顺序和读写操作
 */
internal abstract class OptionPack(private val givenLength: Int) : DataUnit() {
    internal abstract val knownOptions: List<Option> // 已识别选项列表
    internal var unknownOptions = listOf<UnknownOption>() // 未知选项列表
    internal val allOptions: List<Option>
        get() = knownOptions + unknownOptions // 所有选项集合

    internal var order: MutableMap<Byte, Int> = mutableMapOf() // 选项顺序映射

    override val length: Int
        get() = allOptions.fold(0) {sum, option -> sum + option.length}

    /**
     * 解析并返回一个选项，具体由子类实现
     */
    protected abstract fun retrieveOption(buffer: ByteBuffer): Option

    /**
     * 保证选项顺序有效，自动分配新选项顺序
     */
    private fun ensureValidOrder() {
        var nextIndex = order.values.maxOrNull() ?: 0

        allOptions.forEach {
            if (!order.containsKey(it.type)) {
                order[it.type] = nextIndex
                nextIndex++
            }
        }
    }

    /**
     * 读取选项集合内容，自动识别已知和未知选项
     */
    override fun read(buffer: ByteBuffer) {
        var remaining = givenLength - length

        val currentOrder = mutableMapOf<Byte, Int>()
        val currentUnknownOptions = mutableListOf<UnknownOption>()

        var i = 0
        while (true) {
            assertAlways(remaining >= 0)
            if (remaining == 0) {
                break
            }

            retrieveOption(buffer).also {
                // 如果选项类型重复，优先保留最后一个
                currentOrder[it.type] = i
                remaining -= it.length

                if (it is UnknownOption) {
                    currentUnknownOptions.add(it)
                }
             }

            i++
        }

        order = currentOrder
        unknownOptions = currentUnknownOptions.toList()
    }

    /**
     * 写入选项集合内容，按顺序序列化所有选项
     */
    override fun write(buffer: ByteBuffer) {
        ensureValidOrder()

        allOptions.sortedBy { option -> order[option.type] }.forEach { it.write(buffer) }
    }
}