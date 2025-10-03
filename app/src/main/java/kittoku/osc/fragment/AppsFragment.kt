/**
 * 文件：app/src/main/java/kittoku/osc/fragment/AppsFragment.kt
 * 作用说明：
 * 本文件定义了 AppsFragment，用于在设置界面中展示和管理允许路由的应用列表，用户可通过勾选来选择哪些应用可通过 VPN 路由。
 * 主要功能包括：动态生成应用勾选项、批量允许/禁止所有应用、保存用户选择、清理临时偏好项。
 * 在本软件项目中，设置相关界面（如主设置界面）会调用该文件，用于配置 VPN 路由的应用过滤功能。
 * 本文件会调用 kittoku.osc.preference.accessor（用于读写偏好设置）、kittoku.osc.preference.getInstalledAppInfos（获取已安装应用信息）、kittoku.osc.extension.removeTemporaryPreferences（清理临时偏好项）等模块。
 * 本文件依赖标准库 SharedPreferences、PackageManager 及 AndroidX Preference 库。
 */

package kittoku.osc.fragment

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import kittoku.osc.R
import kittoku.osc.extension.removeTemporaryPreferences
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.TEMP_KEY_HEADER
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.setSetPrefValue
import kittoku.osc.preference.getInstalledAppInfos

/**
 * 用于展示和管理允许路由的应用列表的 Fragment
 */
internal class AppsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences

    /**
     * 初始化偏好界面，加载应用列表
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.blank_preference, rootKey)
        setHasOptionsMenu(true)
        prefs = preferenceManager.sharedPreferences!!

        retrieveEachAppPreference(requireContext().applicationContext.packageManager)
    }

    /**
     * 动态生成每个已安装应用的勾选项
     */
    private fun retrieveEachAppPreference(pm: PackageManager) {
        val allowed = getSetPrefValue(OscPrefKey.ROUTE_ALLOWED_APPS, prefs)

        getInstalledAppInfos(pm).forEach { info ->
            val checkBox = CheckBoxPreference(requireContext()).also {
                it.key = TEMP_KEY_HEADER + info.packageName
                it.icon = pm.getApplicationIcon(info)
                it.title = pm.getApplicationLabel(info)
                it.isChecked = allowed.contains(info.packageName)
            }

            preferenceScreen.addPreference(checkBox)
        }
    }

    /**
     * 遍历所有 CheckBoxPreference 并执行指定操作
     */
    private fun processCurrentPreferences(f: (CheckBoxPreference) -> Unit) {
        (0 until preferenceScreen.preferenceCount).forEach { i ->
            val pref = preferenceScreen.getPreference(i)
            if (pref is CheckBoxPreference) {
                f(pref)
            }
        }
    }

    /**
     * 批量修改所有应用勾选状态
     */
    private fun changeAllPreferencesStates(newState: Boolean) {
        processCurrentPreferences {
            it.isChecked = newState
        }
    }

    /**
     * 记录当前已允许的应用到持久化偏好
     */
    private fun memorizeAllowedApps() {
        val allowed = mutableSetOf<String>()

        // 只记录当前已安装且被勾选的应用
        processCurrentPreferences {
            if (it.isChecked) {
                allowed.add(it.key.substring(TEMP_KEY_HEADER.length))
            }
        }

        setSetPrefValue(allowed, OscPrefKey.ROUTE_ALLOWED_APPS, prefs)
    }

    /**
     * 加载菜单项
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        MenuInflater(requireContext()).inflate(R.menu.apps_menu, menu)
    }

    /**
     * 响应菜单点击事件，批量允许/禁止所有应用
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.allow_all -> changeAllPreferencesStates(true)
            R.id.disallow_all -> changeAllPreferencesStates(false)
        }

        return true
    }

    /**
     * Fragment 销毁时清理临时偏好并保存用户选择
     */
    override fun onDestroy() {
        super.onDestroy()

        prefs.removeTemporaryPreferences()
        memorizeAllowedApps()
    }
}