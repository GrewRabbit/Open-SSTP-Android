/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/ConnectorPreference.kt
 * 作用说明：
 * 本文件定义了 HomeConnectorPreference 类，用于在偏好设置界面中展示和管理“主页连接器”开关项。
 * 主要功能：通过 SwitchPreferenceCompat 控件，读取和同步 SharedPreferences 中 HOME_CONNECTOR 配置项的布尔值，实现开关状态的自动更新。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 HomeConnectorPreference 类，以实现对“主页连接器”功能的展示和交互。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、getBooleanPrefValue（布尔值读取工具）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue

/**
 * HomeConnectorPreference
 * 用于“主页连接器”配置项的开关偏好，负责同步 SharedPreferences 中的布尔值并自动更新视图。
 */
internal class HomeConnectorPreference(context: Context, attrs: AttributeSet) : SwitchPreferenceCompat(context, attrs), OscPreference {
    // 当前偏好项对应的配置项枚举
    override val oscPrefKey = OscPrefKey.HOME_CONNECTOR
    // 父依赖项（无）
    override val parentKey: OscPrefKey? = null
    // 偏好项标题（此处为空，实际可根据需求设置）
    override val preferenceTitle = ""

    /**
     * updateView方法
     * 同步开关状态与配置项数据
     */
    override fun updateView() {
        isChecked = getBooleanPrefValue(oscPrefKey, sharedPreferences!!)
    }

    // 监听 SharedPreferences 变化，自动更新视图
    private var listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == oscPrefKey.name) {
            updateView()
        }
    }

    /**
     * onAttached方法
     * 当偏好项附加到界面时注册监听器
     */
    override fun onAttached() {
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * onDetached方法
     * 当偏好项从界面分离时注销监听器
     */
    override fun onDetached() {
        sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(listener)
    }
}