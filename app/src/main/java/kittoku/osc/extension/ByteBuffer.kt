/**
 * 文件：app/src/main/java/kittoku/osc/extension/ByteBuffer.kt
 * 作用说明：
 * 本文件为 ByteBuffer 提供扩展方法，主要实现了缓冲区指针移动、填充、探测、滑动等工具函数，便于协议数据的高效读写和处理。
 * 在本软件项目中，协议解析、数据单元处理等底层模块（如 DataUnit、PPPClient、SstpClient 等）会调用该文件，用于操作网络协议相关的字节缓冲区，提升数据处理的灵活性和安全性。
 * 本文件仅依赖标准库 ByteBuffer，不主动调用其他业务模块。
 */

package kittoku.osc.extension

import java.nio.ByteBuffer

/**
 * 移动 ByteBuffer 的 position 指针
 * @param diff 移动的偏移量
 */
internal fun ByteBuffer.move(diff: Int) {
    position(position() + diff)
}

/**
 * 在 ByteBuffer 中填充指定数量的 0 字节
 * @param size 填充字节数
 */
internal fun ByteBuffer.padZeroByte(size: Int) {
    repeat(size) { put(0) }
}

/**
 * 探测当前位置偏移 diff 的字节值，但不移动指针
 * @param diff 偏移量
 * @return 探测到的字节
 */
internal fun ByteBuffer.probeByte(diff: Int): Byte {
    return this.get(this.position() + diff)
}

/**
 * 探测当前位置偏移 diff 的短整型值，但不移动指针
 * @param diff 偏移量
 * @return 探测到的 Short 值
 */
internal fun ByteBuffer.probeShort(diff: Int): Short {
    return this.getShort(this.position() + diff)
}

/**
 * 获取 ByteBuffer limit 后剩余的容量
 */
internal val ByteBuffer.capacityAfterLimit: Int
    get() = this.capacity() - this.limit()

/**
 * 滑动 ByteBuffer，将剩余数据移到数组头部，并重置 position/limit
 */
internal fun ByteBuffer.slide() {
    val remaining = this.remaining()

    this.array().also {
        it.copyInto(it, 0, this.position(), this.limit())
    }

    this.position(0)
    this.limit(remaining)
}