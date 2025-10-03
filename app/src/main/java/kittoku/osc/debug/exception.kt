/**
 * 文件：app/src/main/java/kittoku/osc/debug/exception.kt
 * 作用说明：
 * 本文件实现了协议数据单元解析异常类 ParsingDataUnitException 及断言工具函数 assertAlways，用于调试和错误处理。
 * 在本软件项目中，协议解析相关模块（如 DataUnit、PPPClient、SstpClient 等）在解析数据单元失败时会抛出该异常，便于定位协议解析错误。
 * assertAlways 用于强制断言条件，主要被各协议处理流程调用，保证关键逻辑的正确性，若断言失败则抛出 AssertionError。
 * 本文件仅依赖标准库 Exception 和 AssertionError，不主动调用其他业务模块。
 */

package kittoku.osc.debug

// 协议数据单元解析异常，解析失败时抛出
internal class ParsingDataUnitException : Exception("Failed to parse data unit")

/**
 * 强制断言工具函数，断言失败时抛出 AssertionError
 * @param value 断言条件
 */
internal fun assertAlways(value: Boolean) {
    if (!value) {
        throw AssertionError()
    }
}