/**
 * 文件：app/src/main/java/kittoku/osc/extension/ByteArray.kt
 * 作用说明：
 * 本文件为 ByteArray 和 String 提供扩展方法，主要实现了字节数组与十六进制字符串之间的转换工具函数。
 * 在本软件项目中，协议数据处理、调试日志、数据单元捕获等模块（如 DataUnit、PPPClient、SstpClient、DataUnitCapture 等）会调用该文件，用于格式化和解析网络协议中的字节数据，便于调试和数据展示。
 * 本文件仅依赖标准库，不主动调用其他业务模块。
 */

package kittoku.osc.extension

/**
 * 将 ByteArray 转换为十六进制字符串
 * @param parse 是否每16字节换行，便于调试展示
 * @return 十六进制字符串
 */
internal fun ByteArray.toHexString(parse: Boolean = false): String {
    var output = ""

    forEachIndexed { index, byte ->
        output += String.format("%02X", byte.toInt() and 0xFF)

        if (parse) output += if (index % 16 == 15) "\n" else " "
    }

    return output
}

/**
 * 将十六进制字符串转换为 ByteArray
 * @throws Exception 字符串长度不是偶数时抛出异常
 * @return 转换后的字节数组
 */
internal fun String.toHexByteArray(): ByteArray {
    if (length % 2 != 0) throw Exception("Fragmented Byte")

    val arrayLength = length / 2
    val output = ByteArray(arrayLength)

    repeat(arrayLength) {
        val start = it * 2
        output[it] = this.slice(start..start + 1).toInt(16).toByte()
    }

    return output
}