/**
 * 文件：app/src/main/java/kittoku/osc/extension/Byte.kt
 * 作用说明：
 * 本文件为字节类型 Byte 提供扩展方法，主要实现了将 Byte 按无符号字节（UByte）转换为 Int 的工具函数。
 * 在本软件项目中，协议解析、数据处理等底层模块（如 PPPClient、SstpClient、DataUnit 等）会调用该文件，用于处理网络协议中的字节数据，确保字节转换时不受符号位影响。
 * 本文件仅依赖标准库，不主动调用其他业务模块。
 */

package kittoku.osc.extension

/**
 * 将 Byte 按无符号字节（UByte）转换为 Int
 * @return 转换后的 Int 值（0~255）
 */
internal fun Byte.toIntAsUByte(): Int {
    return this.toInt() and 0x000000FF
}