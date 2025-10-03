/**
 * 文件：app/src/main/java/kittoku/osc/fragment/ProfilesFragment.kt
 * 作用说明：
 * 本文件定义了 ProfilesFragment，用于在设置界面中展示和管理已保存的 VPN 配置文件（Profile），支持加载、删除 Profile。
 * 主要功能包括：动态生成配置文件列表、弹窗确认加载/删除、导入配置到偏好设置、清理临时偏好项。
 * 在本软件项目中，设置相关界面（如主设置界面）会调用该文件，用于实现 VPN 配置文件的选择与管理功能。
 * 本文件会调用 kittoku.osc.preference.importProfile（导入配置）、kittoku.osc.preference.summarizeProfile（摘要显示配置内容）、kittoku.osc.extension.removeTemporaryPreferences（清理临时偏好项）等模块。
 * 本文件依赖标准库 SharedPreferences、Activity、Toast 及 AndroidX Preference、AlertDialog 库。
 */

package kittoku.osc.fragment

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kittoku.osc.R
import kittoku.osc.extension.removeTemporaryPreferences
import kittoku.osc.preference.PROFILE_KEY_HEADER
import kittoku.osc.preference.importProfile
import kittoku.osc.preference.summarizeProfile

/**
 * 用于展示和管理已保存 VPN 配置文件的 Fragment
 */
internal class ProfilesFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences
    private var dialogResource = 0

    /**
     * 初始化偏好界面，加载配置文件列表
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.blank_preference, rootKey)
        setHasOptionsMenu(true)
        prefs = preferenceManager.sharedPreferences!!
        dialogResource = EditTextPreference(requireContext()).dialogLayoutResource

        retrieveEachProfile()
    }

    /**
     * 动态生成每个已保存配置文件的选项
     */
    private fun retrieveEachProfile() {
        prefs.all.filter { it.key.startsWith(PROFILE_KEY_HEADER) }.forEach { entry ->
            Preference(requireContext()).also {
                it.key = "_" + entry.key
                it.title = entry.key.substringAfter(PROFILE_KEY_HEADER)
                it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    showLoadDialog(entry.key)
                    true
                }
                preferenceScreen.addPreference(it)
            }
        }
    }

    /**
     * 弹窗显示配置文件详情，支持加载/删除
     * @param key 配置文件对应的 SharedPreferences 键
     */
    private fun showLoadDialog(key: String) {
        val profile = prefs.getString(key, null)!!

        AlertDialog.Builder(requireContext()).also {
            it.setTitle(key.substringAfter(PROFILE_KEY_HEADER))
            it.setMessage(summarizeProfile(profile))

            // 加载配置文件
            it.setPositiveButton("LOAD") { _, _ ->
                importProfile(profile, prefs)
                Toast.makeText(requireContext(), "PROFILE LOADED", Toast.LENGTH_SHORT).show()
                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
            }

            // 取消操作
            it.setNegativeButton("CANCEL") { _, _ -> }

            // 删除配置文件
            it.setNeutralButton("DELETE") { _, _ ->
                prefs.edit().also { editor ->
                    editor.remove(key)
                    editor.apply()
                }
                preferenceScreen.removePreference(findPreference("_$key")!!)
                Toast.makeText(requireContext(), "PROFILE DELETED", Toast.LENGTH_SHORT).show()
            }

            it.show()
        }
    }

    /**
     * Fragment 销毁时清理临时偏好项
     */
    override fun onDestroy() {
        super.onDestroy()
        prefs.removeTemporaryPreferences()
    }
}