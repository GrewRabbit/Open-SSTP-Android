/**
 * 文件：app/src/main/java/kittoku/osc/preference/accessor/uri.kt
 * 作用说明：
 * 本文件封装了对 SharedPreferences 中 URI 类型配置项的读取和写入操作，提供统一的访问接口。
 * 主要功能包括：根据配置项键获取 URI 值、设置 URI 值到 SharedPreferences。
 * 在本项目中，配置管理相关模块（如 PreferenceFragment、配置初始化逻辑等）会调用本文件的方法以读写 URI 类型的用户设置。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、DEFAULT_URI_MAP（默认值映射）、toUri 扩展方法等模块。
 * 依赖 Android SDK 的 SharedPreferences、Uri 及项目内的配置项相关定义和扩展方法。
 */

package kittoku.osc.preference.accessor

import android.content.SharedPreferences
import android.net.Uri
import kittoku.osc.extension.toUri
import kittoku.osc.preference.DEFAULT_URI_MAP
import kittoku.osc.preference.OscPrefKey

/**
 * 获取指定配置项的 URI 值
 * @param key 配置项枚举
 * @param prefs SharedPreferences 实例
 * @return URI 对象，若无则返回默认值
 */
internal fun getURIPrefValue(key: OscPrefKey, prefs: SharedPreferences): Uri? {
    return prefs.getString(key.name, null)?.toUri() ?: DEFAULT_URI_MAP[key]
}

/**
 * 设置指定配置项的 URI 值
 * @param value 要设置的 URI 对象
 * @param key 配置项枚举
 * @param prefs SharedPreferences 实例
 */
internal fun setURIPrefValue(value: Uri?, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putString(key.name, value?.toString() ?: "")
        it.apply()
    }
}