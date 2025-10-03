/**
 * 文件：app/src/main/java/kittoku/osc/preference/accessor/set.kt
 * 作用说明：
 * 本文件封装了对 SharedPreferences 中字符串集合类型配置项的读取和写入操作，提供统一的访问接口。
 * 主要功能包括：根据配置项键获取字符串集合、设置字符串集合到 SharedPreferences。
 * 在本项目中，配置管理相关模块（如 PreferenceFragment、配置初始化逻辑等）会调用本文件的方法以读写集合类型的用户设置。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、DEFAULT_SET_MAP（默认值映射）等模块。
 * 依赖 Android SDK 的 SharedPreferences 及项目内的配置项相关定义。
 */

package kittoku.osc.preference.accessor

import android.content.SharedPreferences
import kittoku.osc.preference.DEFAULT_SET_MAP
import kittoku.osc.preference.OscPrefKey

/**
 * 获取指定配置项的字符串集合
 * @param key 配置项枚举
 * @param prefs SharedPreferences 实例
 * @return 字符串集合，若无则返回默认值
 */
internal fun getSetPrefValue(key: OscPrefKey, prefs: SharedPreferences): Set<String> {
    return prefs.getStringSet(key.name, DEFAULT_SET_MAP[key]!!)!!
}

/**
 * 设置指定配置项的字符串集合
 * @param value 要设置的字符串集合
 * @param key 配置项枚举
 * @param prefs SharedPreferences 实例
 */
internal fun setSetPrefValue(value: Set<String>, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putStringSet(key.name, value)
        it.apply()
    }
}