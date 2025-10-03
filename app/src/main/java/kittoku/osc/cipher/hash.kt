/**
 * 文件：hash.kt
 * 作用说明：
 * 本文件实现了 MD4 哈希算法的核心逻辑，主要用于对输入数据进行哈希处理，常用于加密、认证等场景。
 * 在本软件项目中，凡是需要进行 MD4 哈希计算的模块都会调用本文件的 `hashMd4` 方法，例如认证、密钥生成等相关功能模块。
 * 本文件主要作为工具类被其他加密相关文件或业务逻辑文件调用，内部未主动调用其他项目文件，仅依赖标准库。
 */

package kittoku.osc.cipher

import java.nio.ByteBuffer
import java.nio.IntBuffer

// Int 类型字节序反转，用于 MD4 算法中的数据处理
private fun Int.reversed(): Int {
    val bytes = ByteBuffer.allocate(4)
    bytes.putInt(this)
    bytes.array().reverse()
    return bytes.getInt(0)
}

// Long 类型字节序反转，用于 MD4 算法中的数据处理
private fun Long.reversed(): Long {
    val bytes = ByteBuffer.allocate(8)
    bytes.putLong(this)
    bytes.array().reverse()
    return bytes.getLong(0)
}

/**
 * MD4 哈希算法实现
 * @param input 需要哈希的字节数组
 * @return 哈希后的字节数组（长度16字节）
 */
internal fun hashMd4(input: ByteArray): ByteArray {
    val inputLength = input.size
    val remainder = inputLength % 64
    // 计算填充长度
    val padLength = if (remainder >= 56) 64 - (remainder - 56) else (56 - remainder)
    val bytesLength = inputLength + padLength + 8

    val bytes = ByteBuffer.allocate(bytesLength)
    bytes.put(input)
    // 添加MD4填充起始字节
    bytes.put(0b1000_0000.toByte())
    bytes.position(bytesLength - 8)
    // 添加长度信息（位数，字节序反转）
    bytes.putLong((8 * inputLength).toLong().reversed())
    bytes.clear()

    // 初始化MD4算法常量
    var A = 0x01234567.reversed()
    var B = 0x89ABCDEF.toInt().reversed()
    var C = 0xFEDCBA98.toInt().reversed()
    var D = 0x76543210.reversed()

    // MD4算法的三种基本函数
    val F = { X: Int, Y: Int, Z: Int -> (X and Y) or (X.inv() and Z) }
    val G = { X: Int, Y: Int, Z: Int -> (X and Y) or (Y and Z) or (Z and X) }
    val H = { X: Int, Y: Int, Z: Int -> X xor Y xor Z }
    // 循环左移
    val rotate = { X: Int, s: Int -> X.shl(s) or X.ushr(32 - s) }

    val ints = IntBuffer.allocate(16)

    // MD4每轮的非线性变换
    val nl_1 = { a: Int, b: Int, c: Int, d: Int, k: Int, s: Int ->
        rotate(a + F(b, c, d) + ints.get(k), s)
    }
    val nl_2 = { a: Int, b: Int, c: Int, d: Int, k: Int, s: Int ->
        rotate(a + G(b, c, d) + ints.get(k) + 0x5A827999, s)
    }
    val nl_3 = { a: Int, b: Int, c: Int, d: Int, k: Int, s: Int ->
        rotate(a + H(b, c, d) + ints.get(k) + 0x6ED9EBA1, s)
    }

    // 按块处理输入数据
    repeat(bytesLength / 64) {
        val AA = A
        val BB = B
        val CC = C
        val DD = D

        ints.clear()
        // 读取每块数据并字节序反转
        repeat(16) { ints.put(bytes.int.reversed()) }

        // 第一轮变换
        A = nl_1(A, B, C, D, 0, 3)
        D = nl_1(D, A, B, C, 1, 7)
        C = nl_1(C, D, A, B, 2, 11)
        B = nl_1(B, C, D, A, 3, 19)
        A = nl_1(A, B, C, D, 4, 3)
        D = nl_1(D, A, B, C, 5, 7)
        C = nl_1(C, D, A, B, 6, 11)
        B = nl_1(B, C, D, A, 7, 19)
        A = nl_1(A, B, C, D, 8, 3)
        D = nl_1(D, A, B, C, 9, 7)
        C = nl_1(C, D, A, B, 10, 11)
        B = nl_1(B, C, D, A, 11, 19)
        A = nl_1(A, B, C, D, 12, 3)
        D = nl_1(D, A, B, C, 13, 7)
        C = nl_1(C, D, A, B, 14, 11)
        B = nl_1(B, C, D, A, 15, 19)

        // 第二轮变换
        A = nl_2(A, B, C, D, 0, 3)
        D = nl_2(D, A, B, C, 4, 5)
        C = nl_2(C, D, A, B, 8, 9)
        B = nl_2(B, C, D, A, 12, 13)
        A = nl_2(A, B, C, D, 1, 3)
        D = nl_2(D, A, B, C, 5, 5)
        C = nl_2(C, D, A, B, 9, 9)
        B = nl_2(B, C, D, A, 13, 13)
        A = nl_2(A, B, C, D, 2, 3)
        D = nl_2(D, A, B, C, 6, 5)
        C = nl_2(C, D, A, B, 10, 9)
        B = nl_2(B, C, D, A, 14, 13)
        A = nl_2(A, B, C, D, 3, 3)
        D = nl_2(D, A, B, C, 7, 5)
        C = nl_2(C, D, A, B, 11, 9)
        B = nl_2(B, C, D, A, 15, 13)

        // 第三轮变换
        A = nl_3(A, B, C, D, 0, 3)
        D = nl_3(D, A, B, C, 8, 9)
        C = nl_3(C, D, A, B, 4, 11)
        B = nl_3(B, C, D, A, 12, 15)
        A = nl_3(A, B, C, D, 2, 3)
        D = nl_3(D, A, B, C, 10, 9)
        C = nl_3(C, D, A, B, 6, 11)
        B = nl_3(B, C, D, A, 14, 15)
        A = nl_3(A, B, C, D, 1, 3)
        D = nl_3(D, A, B, C, 9, 9)
        C = nl_3(C, D, A, B, 5, 11)
        B = nl_3(B, C, D, A, 13, 15)
        A = nl_3(A, B, C, D, 3, 3)
        D = nl_3(D, A, B, C, 11, 9)
        C = nl_3(C, D, A, B, 7, 11)
        B = nl_3(B, C, D, A, 15, 15)

        // 累加结果
        A += AA
        B += BB
        C += CC
        D += DD
    }

    // 输出最终哈希值
    bytes.clear()
    bytes.putInt(A.reversed())
    bytes.putInt(B.reversed())
    bytes.putInt(C.reversed())
    bytes.putInt(D.reversed())

    return bytes.array().sliceArray(0..15)
}
