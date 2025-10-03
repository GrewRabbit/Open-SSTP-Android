/**
 * 文件：app/src/main/java/kittoku/osc/control/LogWriter.kt
 * 作用说明：
 * 本文件实现了日志写入工具类 LogWriter，用于将 VPN 运行过程中的日志消息写入指定输出流（如文件或内存流）。
 * 在本软件项目中，Controller、OscVpnService 等主流程控制模块会调用该文件，用于记录连接、认证、异常、断开等关键事件日志，便于问题排查和运行监控。
 * LogWriter 通过互斥锁保证多协程环境下日志写入的线程安全，支持同步和异步写入，并负责流的关闭和刷新。
 * 本文件不会主动调用其他业务模块，仅依赖标准库和协程工具。
 */

package kittoku.osc.control

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 日志写入工具类，负责将日志消息写入输出流
internal class LogWriter(logOutput: OutputStream) {
    // 写入互斥锁，保证多协程安全
    private val mutex = Mutex()
    // 日志输出流，带缓冲
    private val outputStream = BufferedOutputStream(logOutput)
    // 获取当前时间字符串，格式化为 yyyy-MM-dd HH:mm:ss.SSS
    private val currentTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    /**
     * 同步写入日志消息（无锁），适用于不会有并发的场景，如 onCreate/onDestroy
     * @param message 日志内容
     */
    internal fun write(message: String) {
        outputStream.write("[$currentTime] $message\n".toByteArray(Charsets.UTF_8))
    }

    /**
     * 异步写入日志消息（加锁），适用于多协程并发场景
     * @param message 日志内容
     */
    internal suspend fun report(message: String) {
        mutex.withLock { write(message) }
    }

    /**
     * 刷新并关闭日志输出流
     */
    internal fun close() {
        outputStream.flush()
        outputStream.close()
    }
}