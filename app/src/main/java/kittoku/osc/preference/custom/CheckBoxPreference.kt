/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/CheckBoxPreference.kt
 * 作用说明：
 * 本文件用于自定义项目中的复选框偏好项（CheckBoxPreference），为配置项提供统一的视图和行为扩展。
 * 主要功能包括：根据配置项的键自动同步复选框状态、初始化偏好项、定义具体的配置项复选框。
 * 在本项目中，偏好设置界面相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的类和方法，以实现对布尔类型配置项的展示和交互。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、getBooleanPrefValue（布尔值读取工具）等模块。
 * 依赖 Android SDK 的 Preference 相关组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue

/**
 * ModifiedCheckBoxPreference
 * 自定义复选框偏好项基类，扩展了 CheckBoxPreference 并实现 OscPreference 接口。
 * 负责根据配置项同步复选框状态，并在附加到界面时初始化。
 */
internal abstract class ModifiedCheckBoxPreference(context: Context, attrs: AttributeSet) : CheckBoxPreference(context, attrs), OscPreference {
    /**
     * 更新视图，将复选框状态与配置项同步
     */
    override fun updateView() {
        isChecked = getBooleanPrefValue(oscPrefKey, sharedPreferences!!)
    }

    /**
     * 当偏好项附加到界面时初始化
     */
    override fun onAttached() {
        initialize()
    }
}

/**
 * SSLDoVerifyPreference
 * 用于“验证主机名”配置项的复选框偏好，控制 SSL 主机名校验功能。
 */
internal class SSLDoVerifyPreference(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_DO_VERIFY
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Verify Hostname"
}

/**
 * PPPIPv4EnabledPreference
 * 用于“启用 IPv4”配置项的复选框偏好，控制 PPP IPv4 功能开关。
 */
internal class PPPIPv4EnabledPreference(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_IPv4_ENABLED
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Enable IPv4"
}

/**
 * PPPIPv6EnabledPreference
 * 用于“启用 IPv6”配置项的复选框偏好，控制 PPP IPv6 功能开关。
 */
internal class PPPIPv6EnabledPreference(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_IPv6_ENABLED
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Enable IPv6"
}

/**
 * RouteDoAddDefaultRoutePreference
 * 用于“添加默认路由”配置项的复选框偏好，控制是否自动添加默认路由。
 */
internal class RouteDoAddDefaultRoutePreference(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Add Default Route"
}

/**
 * RouteDoRoutePrivateAddresses
 * 用于“路由私有/唯一本地地址”配置项的复选框偏好，控制是否路由私有地址。
 */
internal class RouteDoRoutePrivateAddresses(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_DO_ROUTE_PRIVATE_ADDRESSES
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Route Private/Unique-Local Addresses"
}