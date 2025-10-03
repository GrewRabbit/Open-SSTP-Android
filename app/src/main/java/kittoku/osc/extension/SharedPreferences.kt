/**
 * 文件：app/src/main/java/kittoku/osc/extension/SharedPreferences.kt
 * 作用说明：
 * 本文件为 SharedPreferences 提供扩展方法，主要实现了批量移除临时偏好设置项的工具函数 removeTemporaryPreferences。
 * 在本软件项目中，偏好设置管理、临时参数清理等模块（如 OscVpnService、设置界面相关模块）会调用该文件，用于清理以 TEMP_KEY_HEADER 开头的临时配置项，保证偏好数据的整洁性。
 * 本文件依赖标准库 SharedPreferences 及项目常量 TEMP_KEY_HEADER，不主动调用其他业务模块。
 */

package kittoku.osc.extension

import android.content.SharedPreferences
import kittoku.osc.preference.TEMP_KEY_HEADER

/**
 * 移除所有以 TEMP_KEY_HEADER 开头的临时偏好设置项
 */
internal fun SharedPreferences.removeTemporaryPreferences() {
    val editor = edit()

    all.keys.filter { it.startsWith(TEMP_KEY_HEADER) }.forEach { editor.remove(it) }

    editor.apply()
}