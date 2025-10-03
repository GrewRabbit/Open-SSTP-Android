/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/SummaryPreference.kt
 * 作用说明：
 * 本文件用于定义“摘要展示”类型的偏好项（SummaryPreference），如连接状态、允许的应用列表等信息展示控件。
 * 主要功能：通过自定义 SummaryPreference 控件，动态展示 SharedPreferences 中的状态或集合信息，供用户在设置界面查看当前状态或已选项。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 HomeStatusPreference、RouteAllowedAppsPreference，以实现状态和集合摘要展示功能。
 * 本文件会调用 OscPrefKey（配置项枚举）、getStringPrefValue、getSetPrefValue（配置项读取工具）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue

/**
 * SummaryPreference
 * 摘要展示偏好项基类，负责监听配置项变化并更新摘要内容，支持多行摘要显示。
 * @param context 上下文环境
 * @param attrs 属性集
 */
internal abstract class SummaryPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs), OscPreference {
    // 监听配置项变化，自动更新视图
    protected open val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == oscPrefKey.name) {
            updateView()
        }
    }

    /**
     * 当偏好项附加到界面时，注册监听器并初始化
     */
    override fun onAttached() {
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(listener)
        initialize()
    }

    /**
     * 当偏好项从界面移除时，注销监听器
     */
    override fun onDetached() {
        sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 绑定视图时，设置摘要最大显示行数为无限
     */
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.findViewById(android.R.id.summary)?.also {
            it as TextView
            it.maxLines = Int.MAX_VALUE
        }
    }
}

/**
 * HomeStatusPreference
 * 用于展示“当前连接状态”配置项的摘要偏好项。
 * @param context 上下文环境
 * @param attrs 属性集
 */
internal class HomeStatusPreference(context: Context, attrs: AttributeSet) : SummaryPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.HOME_STATUS
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Current Status"

    /**
     * 更新摘要内容，显示当前连接状态或未连接提示
     */
    override fun updateView() {
        summary = getStringPrefValue(oscPrefKey, sharedPreferences!!).ifEmpty { "[No Connection Established]" }
    }
}

/**
 * RouteAllowedAppsPreference
 * 用于展示“允许的应用列表”配置项的摘要偏好项，依赖于是否启用应用规则。
 * @param context 上下文环境
 * @param attrs 属性集
 */
internal class RouteAllowedAppsPreference(context: Context, attrs: AttributeSet) : SummaryPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_ALLOWED_APPS
    override val parentKey = OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE
    override val preferenceTitle = "Select Allowed Apps"

    /**
     * 更新摘要内容，显示已选应用数量
     */
    override fun updateView() {
        summary = when (val size = getSetPrefValue(oscPrefKey, sharedPreferences!!).size) {
            0 -> "[No App Selected]"
            1 -> "[1 App Selected]"
            else -> "[$size Apps Selected]"
        }
    }
}