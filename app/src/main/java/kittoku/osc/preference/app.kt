/**
 * 文件：app/src/main/java/kittoku/osc/preference/app.kt
 * 作用说明：
 * 本文件用于管理和获取设备已安装应用的信息，主要用于偏好设置中“允许的应用列表”功能（如 VPN 路由规则按应用过滤）。
 * 主要功能：
 * - getInstalledAppInfos：获取所有可启动的已安装应用信息，避免重复（如 Google Quick Search Box）。
 * - getValidAllowedAppInfos：根据用户偏好设置，筛选当前已安装且被允许的应用信息。
 * 在本项目中，偏好设置相关模块（如 RouteAllowedAppsPreference、PreferenceFragment 等）会调用本文件的方法，以实现应用选择和展示功能。
 * 本文件会调用 Android SDK 的 PackageManager、SharedPreferences 及项目内的 getSetPrefValue 工具方法。
 * 依赖 Android SDK 的应用信息管理组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kittoku.osc.preference.accessor.getSetPrefValue

/**
 * AppString
 * 用于封装应用包名和标签信息的数据类。
 */
internal data class AppString(val packageName: String, val label: String)

/**
 * 获取所有可启动的已安装应用信息，去除重复项。
 * @param pm PackageManager 实例
 * @return 可启动应用的 ApplicationInfo 列表
 */
internal fun getInstalledAppInfos(pm: PackageManager): List<ApplicationInfo> {
    val intent = Intent(Intent.ACTION_MAIN).also {
        it.addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val addedPackageNames = mutableSetOf<String>()

    return pm.queryIntentActivities(intent, 0).map { it.activityInfo.applicationInfo }.filter {
        if (addedPackageNames.contains(it.packageName)) { // 解决部分应用重复问题
            false
        } else {
            addedPackageNames.add(it.packageName)
            true
        }
    }
}

/**
 * 获取当前已安装且被允许的应用信息（根据偏好设置筛选）。
 * @param prefs SharedPreferences 实例
 * @param pm PackageManager 实例
 * @return 已安装且被允许的 ApplicationInfo 列表
 */
internal fun getValidAllowedAppInfos(prefs: SharedPreferences, pm: PackageManager): List<ApplicationInfo> {
    // 返回当前已安装的、在允许列表中的应用
    return getSetPrefValue(OscPrefKey.ROUTE_ALLOWED_APPS, prefs).mapNotNull {
        try {
            pm.getApplicationInfo(it, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}