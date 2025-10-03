/**
 * 文件：app/src/main/java/kittoku/osc/extension/String.kt
 * 作用说明：
 * 本文件为 String 类型及字符串相关操作提供扩展方法，主要实现了字符串拼接（sum）和字符串转 Uri（toUri）的工具函数。
 * 在本软件项目中，网络参数处理、配置解析、界面跳转等模块（如 OscVpnService、设置界面、协议相关模块）会调用该文件，用于简化字符串操作和 Uri 解析流程。
 * 本文件依赖标准库 Uri，不主动调用其他业务模块。
 */

package kittoku.osc.extension

import android.net.Uri

/**
 * 拼接多个字符串
 * @param words 需要拼接的字符串列表
 * @return 拼接后的字符串
 */
internal fun sum(vararg words: String): String {
    var result = ""

    words.forEach {
        result += it
    }

    return result
}

/**
 * 将字符串转换为 Uri 对象
 * @return 转换后的 Uri，若字符串为空则返回 null
 */
internal fun String.toUri(): Uri? {
    return if (this.isEmpty()) {
        null
    } else {
        Uri.parse(this)
    }
}