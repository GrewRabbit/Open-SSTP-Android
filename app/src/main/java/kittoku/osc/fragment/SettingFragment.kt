/**
 * 文件：app/src/main/java/kittoku/osc/fragment/SettingFragment.kt
 * 作用说明：
 * 本文件定义了 SettingFragment，用于设置界面管理 VPN 相关的目录选择（证书目录、日志目录）及允许路由的应用配置入口。
 * 主要功能包括：处理证书目录和日志目录的选择与权限获取、跳转到允许路由应用列表界面。
 * 在本软件项目中，主设置界面会调用该文件，用于实现用户对 VPN 证书、日志存储路径的配置，以及允许路由应用的管理入口。
 * 本文件会调用 kittoku.osc.preference.accessor.setURIPrefValue（保存目录 URI）、kittoku.osc.activity.BlankActivity（跳转应用列表）、kittoku.osc.preference.custom.DirectoryPreference（自定义目录选择控件）等模块。
 * 本文件依赖标准库 Activity、Intent、SharedPreferences 及 AndroidX Preference 库。
 */

package kittoku.osc.fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kittoku.osc.R
import kittoku.osc.activity.BLANK_ACTIVITY_TYPE_APPS
import kittoku.osc.activity.BlankActivity
import kittoku.osc.activity.EXTRA_KEY_TYPE
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.setURIPrefValue
import kittoku.osc.preference.custom.DirectoryPreference

/**
 * 设置界面 Fragment，负责证书目录、日志目录选择及允许路由应用入口
 */
internal class SettingFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences

    // 证书目录选择控件
    private lateinit var certDirPref: DirectoryPreference
    // 日志目录选择控件
    private lateinit var logDirPref: DirectoryPreference

    /**
     * 证书目录选择结果回调，保存 URI 并更新视图
     */
    private val certDirLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) result.data?.data?.also {
            requireContext().contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } else null

        setURIPrefValue(uri, OscPrefKey.SSL_CERT_DIR, prefs)
        certDirPref.updateView()
    }

    /**
     * 日志目录选择结果回调，保存 URI 并更新视图
     */
    private val logDirLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) result.data?.data?.also {
            requireContext().contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } else null

        setURIPrefValue(uri, OscPrefKey.LOG_DIR, prefs)
        logDirPref.updateView()
    }

    /**
     * 初始化设置界面，绑定各项监听器
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        prefs = preferenceManager.sharedPreferences!!

        certDirPref = findPreference(OscPrefKey.SSL_CERT_DIR.name)!!
        logDirPref = findPreference(OscPrefKey.LOG_DIR.name)!!

        setCertDirListener()
        setLogDirListener()
        setAllowedAppsListener()
    }

    /**
     * 证书目录点击监听，弹出目录选择器
     */
    private fun setCertDirListener() {
        certDirPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                certDirLauncher.launch(intent)
            }
            true
        }
    }

    /**
     * 日志目录点击监听，弹出目录选择器
     */
    private fun setLogDirListener() {
        logDirPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                logDirLauncher.launch(intent)
            }
            true
        }
    }

    /**
     * 允许路由应用入口点击监听，跳转到应用列表界面
     */
    private fun setAllowedAppsListener() {
        findPreference<Preference>(OscPrefKey.ROUTE_ALLOWED_APPS.name)!!.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(requireContext(), BlankActivity::class.java).putExtra(
                    EXTRA_KEY_TYPE,
                    BLANK_ACTIVITY_TYPE_APPS
                ))
                true
            }
        }
    }
}