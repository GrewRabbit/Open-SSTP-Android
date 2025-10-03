/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/StringPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“字符串输入”类型的偏好项（StringPreference），如主机名、用户名、SNI、静态IP、代理相关等参数的输入控件。
 * 主要功能：通过自定义 StringPreference 控件，展示和同步 SharedPreferences 中字符串配置项，便于用户在设置界面输入和修改相关文本。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的各个 StringPreference 子类，以实现字符串参数的输入功能。
 * 本文件会调用 OscEditTextPreference（自定义文本输入偏好项基类）、OscPrefKey（配置项枚举）、getStringPrefValue（字符串读取工具）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getStringPrefValue

/**
 * StringPreference
 * 字符串输入偏好项基类，继承自 OscEditTextPreference，摘要显示当前输入内容或未输入提示。
 */
internal abstract class StringPreference(context: Context, attrs: AttributeSet) : OscEditTextPreference(context, attrs), OscPreference {
    // 摘要提供器，显示当前输入内容或未输入提示
    override val provider = SummaryProvider<Preference> {
        getStringPrefValue(oscPrefKey, it.sharedPreferences!!).ifEmpty { "[No Value Entered]" }
    }
}

/**
 * HomeHostnamePreference
 * 用于“主机名”配置项的字符串输入偏好项。
 */
internal class HomeHostnamePreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.HOME_HOSTNAME
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Hostname"
}

/**
 * HomeUsernamePreference
 * 用于“用户名”配置项的字符串输入偏好项。
 */
internal class HomeUsernamePreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.HOME_USERNAME
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Username"
}

/**
 * SSLCustomSNIHostnamePreference
 * 用于“自定义SNI主机名”配置项的字符串输入偏好项，依赖于是否启用自定义SNI。
 */
internal class SSLCustomSNIHostnamePreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_CUSTOM_SNI
    override val parentKey = OscPrefKey.SSL_DO_USE_CUSTOM_SNI
    override val preferenceTitle = "Custom SNI Hostname"
}

/**
 * PPPStaticIPv4AddressPreference
 * 用于“静态IPv4地址”配置项的字符串输入偏好项，依赖于是否请求静态IP。
 */
internal class PPPStaticIPv4AddressPreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_STATIC_IPv4_ADDRESS
    override val preferenceTitle = "Static IPv4 Address"
    override val parentKey = OscPrefKey.PPP_DO_REQUEST_STATIC_IPv4_ADDRESS
    override val hint = "192.168.0.1"
}

/**
 * ProxyHostnamePreference
 * 用于“代理主机名”配置项的字符串输入偏好项，依赖于是否启用代理。
 */
internal class ProxyHostnamePreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PROXY_HOSTNAME
    override val parentKey =  OscPrefKey.PROXY_DO_USE_PROXY
    override val preferenceTitle = "Proxy Server Hostname"
}

/**
 * ProxyUsernamePreference
 * 用于“代理用户名”配置项的字符串输入偏好项，依赖于是否启用代理。
 */
internal class ProxyUsernamePreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PROXY_USERNAME
    override val parentKey =  OscPrefKey.PROXY_DO_USE_PROXY
    override val preferenceTitle = "Proxy Username (optional)"
}

/**
 * DNSCustomAddressPreference
 * 用于“自定义DNS地址”配置项的字符串输入偏好项，依赖于是否启用自定义DNS。
 */
internal class DNSCustomAddressPreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.DNS_CUSTOM_ADDRESS
    override val parentKey = OscPrefKey.DNS_DO_USE_CUSTOM_SERVER
    override val preferenceTitle = "Custom DNS Server Address"

    /**
     * 附加到界面时，设置对话框提示信息
     */
    override fun onAttached() {
        dialogMessage = "NOTICE: packets associated with this address is routed to the VPN tunnel"
        super.onAttached()
    }
}

/**
 * RouteCustomRoutesPreference
 * 用于“自定义路由”配置项的字符串输入偏好项，依赖于是否启用自定义路由，支持多行输入。
 */
internal class RouteCustomRoutesPreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_CUSTOM_ROUTES
    override val parentKey = OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES
    override val preferenceTitle = "Edit Custom Routes"
    // 支持多行文本输入
    override val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    override val hint = "192.168.1.0/24\n2001:db8::/32"

    /**
     * 更新视图时，修复 Google Issue 37032278，去除首尾空格
     */
    override fun updateView() {
        text = getStringPrefValue(oscPrefKey, sharedPreferences!!).trim()
    }

    /**
     * 摘要提供器，根据是否输入路由显示不同摘要
     */
    override val provider = SummaryProvider<Preference> {
        val currentValue = getStringPrefValue(oscPrefKey, it.sharedPreferences!!)
        if (currentValue.isEmpty()) {
            "[No Value Entered]"
        } else {
            "[Custom Routes Entered]"
        }
    }
}