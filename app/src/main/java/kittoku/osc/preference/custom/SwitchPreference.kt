/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/SwitchPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“开关类型”偏好项（SwitchPreference），如 SSL、PPP、代理、DNS、路由等功能的启用/禁用开关控件。
 * 主要功能：通过自定义 SwitchPreference 控件，展示和同步 SharedPreferences 中布尔型配置项，便于用户在设置界面切换相关功能。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的各个 SwitchPreference 子类，以实现功能开关。
 * 本文件会调用 OscPrefKey（配置项枚举）、getBooleanPrefValue（布尔值读取工具）、initialize（偏好项初始化工具）等模块。
 * 依赖 Android SDK 的 SwitchPreferenceCompat 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue

/**
 * SwitchPreference
 * 开关类型偏好项基类，负责根据配置项同步开关状态，并在附加到界面时初始化。
 */
internal abstract class SwitchPreference(context: Context, attrs: AttributeSet) : SwitchPreferenceCompat(context, attrs), OscPreference {
    /**
     * 更新视图，将 SharedPreferences 中的布尔值同步为当前开关状态
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
 * SSLDoSpecifyCertPreference
 * 用于“指定受信任证书”配置项的开关偏好项。
 */
internal class SSLDoSpecifyCertPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_DO_SPECIFY_CERT
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Specify Trusted Certificates"
}

/**
 * SSLDoSelectSuitesPreference
 * 用于“仅启用选定加密套件”配置项的开关偏好项。
 */
internal class SSLDoSelectSuitesPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_DO_SELECT_SUITES
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Enable Only Selected Cipher Suites"
}

/**
 * SSLDoUseCustomSNIPreference
 * 用于“使用自定义SNI”配置项的开关偏好项。
 */
internal class SSLDoUseCustomSNIPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_DO_USE_CUSTOM_SNI
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Use Custom SNI"
}

/**
 * PPPDoRequestStaticIPv4AddressPreference
 * 用于“请求静态IPv4地址”配置项的开关偏好项，依赖于 IPv4 启用状态。
 */
internal class PPPDoRequestStaticIPv4AddressPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_DO_REQUEST_STATIC_IPv4_ADDRESS
    override val parentKey = OscPrefKey.PPP_IPv4_ENABLED
    override val preferenceTitle = "Request Static IPv4 Address"
}

/**
 * ProxyDoUseProxyPreference
 * 用于“启用HTTP代理”配置项的开关偏好项。
 */
internal class ProxyDoUseProxyPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PROXY_DO_USE_PROXY
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Use HTTP Proxy"
}

/**
 * DNSDoRequestAddressPreference
 * 用于“请求DNS服务器地址”配置项的开关偏好项。
 */
internal class DNSDoRequestAddressPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.DNS_DO_REQUEST_ADDRESS
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Request DNS Server Address"
}

/**
 * DNSDoUseCustomServerPreference
 * 用于“使用自定义DNS服务器”配置项的开关偏好项。
 */
internal class DNSDoUseCustomServerPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.DNS_DO_USE_CUSTOM_SERVER
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Use Custom DNS Server"
}

/**
 * RouteDoAddCustomRoutesPreference
 * 用于“添加自定义路由”配置项的开关偏好项。
 */
internal class RouteDoAddCustomRoutesPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Add Custom Routes"
}

/**
 * RouteDoEnableAppBasedRulePreference
 * 用于“启用基于应用的路由规则”配置项的开关偏好项。
 */
internal class RouteDoEnableAppBasedRulePreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Enable App-Based Rule"
}

/**
 * ReconnectionEnabledPreference
 * 用于“启用自动重连”配置项的开关偏好项。
 */
internal class ReconnectionEnabledPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.RECONNECTION_ENABLED
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Enable Reconnection"
}

/**
 * LogDoSaveLogPreference
 * 用于“保存日志”配置项的开关偏好项。
 */
internal class LogDoSaveLogPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.LOG_DO_SAVE_LOG
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Save Log"
}