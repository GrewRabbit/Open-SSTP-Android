/**
 * 文件：app/src/main/java/kittoku/osc/preference/profile.kt
 * 作用说明：
 * 本文件用于实现配置文件（Profile）的导出、导入及摘要功能，便于用户保存和恢复偏好设置。
 * 主要功能：
 * - exportProfile：将当前 SharedPreferences 中的所有关键配置项导出为字符串，便于备份或分享。
 * - importProfile：将字符串形式的配置文件导入到 SharedPreferences，实现配置恢复。
 * - summarizeProfile：从配置文件字符串中提取主要信息（主机名、用户名、端口号）并生成摘要。
 * 在本项目中，偏好设置相关模块（如设置导入/导出界面、Profile 管理功能等）会调用本文件的方法，实现配置的保存与恢复。
 * 本文件会调用 kittoku.osc.preference.constant.kt（配置项枚举及默认值）、kittoku.osc.preference.accessor 包下的各类 get/set 工具方法，以及 kittoku.osc.extension.toUri 方法。
 * 依赖 Android SDK 的 SharedPreferences 组件。
 */

package kittoku.osc.preference

import android.content.SharedPreferences
import kittoku.osc.extension.toUri
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.accessor.getURIPrefValue
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setIntPrefValue
import kittoku.osc.preference.accessor.setSetPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kittoku.osc.preference.accessor.setURIPrefValue

// 配置项分隔符定义
private const val RECORD_SEPARATOR = 0x1E.toChar().toString() // 记录分隔符
private const val UNIT_SEPARATOR = 0x1F.toChar().toString()   // 单元分隔符

// 导出/导入时排除的布尔型配置项
private val EXCLUDED_BOOLEAN_PREFERENCES = arrayOf(
    OscPrefKey.ROOT_STATE,
    OscPrefKey.HOME_CONNECTOR,
    OscPrefKey.HOME_STATUS,
)

// 导出/导入时排除的字符串型配置项
private val EXCLUDED_STRING_PREFERENCES = arrayOf(
    OscPrefKey.HOME_STATUS,
)

/**
 * 导出当前配置为字符串
 * @param prefs SharedPreferences 实例
 * @return 配置文件字符串
 */
internal fun exportProfile(prefs: SharedPreferences): String {
    var profile = ""

    // 导出布尔型配置项
    DEFAULT_BOOLEAN_MAP.keys.filter { it !in EXCLUDED_BOOLEAN_PREFERENCES }.forEach {
        profile += it.name + UNIT_SEPARATOR + getBooleanPrefValue(it, prefs).toString() + RECORD_SEPARATOR
    }

    // 导出整型配置项
    DEFAULT_INT_MAP.keys.forEach {
        profile += it.name + UNIT_SEPARATOR + getIntPrefValue(it, prefs).toString() + RECORD_SEPARATOR
    }

    // 导出字符串型配置项
    DEFAULT_STRING_MAP.keys.filter { it !in EXCLUDED_STRING_PREFERENCES }.forEach {
        profile += it.name + UNIT_SEPARATOR + getStringPrefValue(it, prefs) + RECORD_SEPARATOR
    }

    // 导出集合型配置项
    DEFAULT_SET_MAP.keys.forEach {
        profile += it.name + UNIT_SEPARATOR + getSetPrefValue(it, prefs).joinToString(UNIT_SEPARATOR) + RECORD_SEPARATOR
    }

    // 导出 Uri 型配置项
    DEFAULT_URI_MAP.keys.forEach {
        getURIPrefValue(it, prefs)?.also { uri ->
            profile += it.name + UNIT_SEPARATOR + uri.toString() + RECORD_SEPARATOR
        }
    }

    return profile
}

/**
 * 导入配置文件字符串到 SharedPreferences
 * @param profile 配置文件字符串
 * @param prefs SharedPreferences 实例
 */
internal fun importProfile(profile: String?, prefs: SharedPreferences) {
    // 解析配置文件字符串为键值对
    val profileMap = profile?.split(RECORD_SEPARATOR)?.filter { it.isNotEmpty() }?.associate {
        val index = it.indexOf(UNIT_SEPARATOR)
        val key = it.substring(0, index)
        val value = it.substring(index + 1)
        key to value
    } ?: mapOf()

    // 导入布尔型配置项
    DEFAULT_BOOLEAN_MAP.keys.filter { it !in EXCLUDED_BOOLEAN_PREFERENCES }.forEach {
        val value = profileMap[it.name]?.toBooleanStrict() ?: DEFAULT_BOOLEAN_MAP.getValue(it)
        setBooleanPrefValue(value, it, prefs)
    }

    // 导入整型配置项
    DEFAULT_INT_MAP.keys.forEach {
        val value = profileMap[it.name]?.toInt() ?: DEFAULT_INT_MAP.getValue(it)
        setIntPrefValue(value, it, prefs)
    }

    // 导入字符串型配置项
    DEFAULT_STRING_MAP.keys.filter { it !in EXCLUDED_STRING_PREFERENCES }.forEach {
        val value = profileMap[it.name] ?: DEFAULT_STRING_MAP.getValue(it)
        setStringPrefValue(value, it, prefs)
    }

    // 导入集合型配置项
    DEFAULT_SET_MAP.keys.forEach { key ->
        val value = profileMap[key.name]?.split(UNIT_SEPARATOR)?.filter { it.isNotEmpty() }?.toSet() ?: DEFAULT_SET_MAP.getValue(key)
        setSetPrefValue(value, key, prefs)
    }

    // 导入 Uri 型配置项
    DEFAULT_URI_MAP.keys.forEach {
        val value = profileMap[it.name]?.toUri() ?: DEFAULT_URI_MAP.getValue(it)
        setURIPrefValue(value, it, prefs)
    }
}

/**
 * 生成配置文件摘要（主机名、用户名、端口号）
 * @param profile 配置文件字符串
 * @return 摘要字符串
 */
internal fun summarizeProfile(profile: String): String {
    var hostname: String = DEFAULT_STRING_MAP.getValue(OscPrefKey.HOME_HOSTNAME)
    var username: String = DEFAULT_STRING_MAP.getValue(OscPrefKey.HOME_USERNAME)
    var portNumber: String = DEFAULT_INT_MAP.getValue(OscPrefKey.SSL_PORT).toString()

    profile.split(RECORD_SEPARATOR).filter { it.isNotEmpty() }.forEach {
        val index = it.indexOf(UNIT_SEPARATOR)
        val key = it.substring(0, index)
        val value = it.substring(index + 1)

        when (key) {
            OscPrefKey.HOME_HOSTNAME.name -> {
                hostname = value
            }
            OscPrefKey.HOME_USERNAME.name -> {
                username = value
            }
            OscPrefKey.SSL_PORT.name -> {
                portNumber = value
            }
        }
    }

    return "[Hostname]\n$hostname\n\n[Username]\n$username\n\n[Port Number]\n$portNumber"
}