/**
 * 文件：app/src/main/java/kittoku/osc/debug/capture.kt
 * 作用说明：
 * 本文件实现了 DataUnitCapture 数据单元捕获与日志工具，用于调试和记录 VPN 协议数据单元的收发内容（HEX、元信息）。
 * 在本软件项目中，主要由协议处理模块（如 PPPClient、SstpClient、Controller 等）在调试或开发阶段调用该文件，用于捕获和输出协议帧的详细内容，便于问题定位和协议分析。
 * 本文件依赖 DataUnit（协议数据单元抽象）、Android Log 工具、互斥锁、时间格式化等标准库，不主动调用其他业务模块。
 * DataUnitCapture 负责线程安全地格式化和输出数据单元的内容（包括时间、方向、类型、HEX），仅用于调试日志输出，不影响业务流程。
 */

package kittoku.osc.debug

import android.util.Log
import kittoku.osc.DEFAULT_MRU
import kittoku.osc.extension.toHexString
import kittoku.osc.unit.DataUnit
import kotlinx.coroutines.sync.Mutex
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 数据单元方向枚举，标记收发
internal enum class Direction {
    RECEIVED, // 接收
    SENT,     // 发送
}

// 数据单元捕获与日志工具类
internal class DataUnitCapture {
    // 互斥锁，保证多协程安全
    private val mutex = Mutex()
    // 数据缓冲区，最大长度为 DEFAULT_MRU
    private val buffer = ByteBuffer.allocate(DEFAULT_MRU)
    // 当前时间字符串，格式化为 yyyy-MM-dd HH:mm:ss.SSS
    private val currentTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    /**
     * 捕获并输出数据单元日志（HEX+元信息）
     * @param unit 协议数据单元
     * @param direction 收发方向
     */
    internal suspend fun logDataUnit(unit: DataUnit, direction: Direction) {
        mutex.lock() // 加锁，保证线程安全

        val message = mutableListOf(direction.name)

        // 添加元信息
        message.add("[INFO]")
        message.add("time = $currentTime")
        message.add("size = ${unit.length}")
        message.add("class = ${unit::class.java.simpleName}")
        message.add("")

        // 添加 HEX 内容
        message.add("[HEX]")
        buffer.clear()
        unit.write(buffer)
        buffer.flip()
        message.add(buffer.array().sliceArray(0 until unit.length).toHexString(true))
        message.add("")

        // 合并并输出日志
        message.reduce { acc, s -> acc + "\n" + s }.also {
            Log.d("CAPTURE", it)
        }

        mutex.unlock() // 解锁
    }
}