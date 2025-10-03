/**
 * 文件：app/src/main/java/kittoku/osc/preference/accessor/int.kt
 * 作用说明：
 * 本文件封装了对 SharedPreferences 中整型配置项的读取、写入和重置操作，提供统一的访问接口。
 * 主要功能包括：根据配置项键获取整型值、设置整型值到 SharedPreferences、重置重连生命周期参数。
 * 在本项目中，配置管理相关模块（如 PreferenceFragment、配置初始化逻辑等）会调用本文件的方法以读写整型类型的用户设置。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、DEFAULT_INT_MAP（默认值映射）等模块。
 * 依赖 Android SDK 的 SharedPreferences 及项目内的配置项相关定义。
 */

package kittoku.osc.preference.accessor

import android.content.SharedPreferences
import kittoku.osc.preference.DEFAULT_INT_MAP
import kittoku.osc.preference.OscPrefKey

/**
 * 获取指定配置项的整型值
 * @param key 配置项枚举
 * @param prefs SharedPreferences 实例
 * @return 整型值，若无则返回默认值
 */
internal fun getIntPrefValue(key: OscPrefKey, prefs: SharedPreferences): Int {
    return prefs.getString(key.name, null)?.toIntOrNull() ?: DEFAULT_INT_MAP[key]!!
}

/**
 * 设置指定配置项的整型值
 * @param value 要设置的整型值
 * @param key 配置项枚举
 * @param prefs SharedPreferences 实例
 */
internal fun setIntPrefValue(value: Int, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putString(key.name, value.toString())
        it.apply()
    }
}

/**
 * 重置重连生命周期参数，将 RECONNECTION_COUNT 的值赋给 RECONNECTION_LIFE
 * @param prefs SharedPreferences 实例
 */
internal fun resetReconnectionLife(prefs: SharedPreferences) {
    getIntPrefValue(OscPrefKey.RECONNECTION_COUNT, prefs).also {
        setIntPrefValue(it, OscPrefKey.RECONNECTION_LIFE, prefs)
    }
}