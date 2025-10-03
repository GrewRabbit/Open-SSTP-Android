/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/IntPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“整型输入”类型的偏好项（IntPreference），如端口号、MRU、MTU、重连次数等参数的输入控件。
 * 主要功能：通过自定义 IntPreference 控件，展示和同步 SharedPreferences 中整型配置项，便于用户在设置界面输入和修改相关数值。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 IntPreference 及其子类，以实现整型参数的输入功能。
 * 本文件会调用 OscEditTextPreference（自定义文本输入偏好项基类）、OscPrefKey（配置项枚举）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import kittoku.osc.preference.OscPrefKey

/**
 * IntPreference
 * 整型输入偏好项基类，继承自 OscEditTextPreference，强制输入类型为数字。
 */
internal abstract class IntPreference(context: Context, attrs: AttributeSet) : OscEditTextPreference(context, attrs), OscPreference {
    // 设置输入框类型为数字
    override val inputType = InputType.TYPE_CLASS_NUMBER
}

/**
 * SSLPortPreference
 * 用于“SSL端口号”配置项的整型输入偏好项。
 */
internal class SSLPortPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    // 对应配置项枚举
    override val oscPrefKey = OscPrefKey.SSL_PORT
    // 无父依赖项
    override val parentKey: OscPrefKey? = null
    // 偏好项标题
    override val preferenceTitle = "Port Number"
}

/**
 * ProxyPortPreference
 * 用于“代理端口号”配置项的整型输入偏好项，依赖于是否启用代理。
 */
internal class ProxyPortPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PROXY_PORT
    // 依赖于 PROXY_DO_USE_PROXY 配置项
    override val parentKey = OscPrefKey.PROXY_DO_USE_PROXY
    override val preferenceTitle = "Proxy Server Port Number"
}

/**
 * PPPMruPreference
 * 用于“PPP MRU”配置项的整型输入偏好项。
 */
internal class PPPMruPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_MRU
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "MRU"
}

/**
 * PPPMtuPreference
 * 用于“PPP MTU”配置项的整型输入偏好项。
 */
internal class PPPMtuPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_MTU
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "MTU"
}

/**
 * PPPAuthTimeoutPreference
 * 用于“PPP认证超时时间”配置项的整型输入偏好项。
 */
internal class PPPAuthTimeoutPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_AUTH_TIMEOUT
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Timeout Period (second)"
}

/**
 * ReconnectionCountPreference
 * 用于“重连次数”配置项的整型输入偏好项，依赖于是否启用重连。
 */
internal class ReconnectionCountPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.RECONNECTION_COUNT
    override val parentKey = OscPrefKey.RECONNECTION_ENABLED
    override val preferenceTitle = "Retry Count"
}

/**
 * ReconnectionIntervalPreference
 * 用于“重连间隔”配置项的整型输入偏好项，依赖于是否启用重连。
 */
internal class ReconnectionIntervalPreference(context: Context, attrs: AttributeSet) : IntPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.RECONNECTION_INTERVAL
    override val parentKey = OscPrefKey.RECONNECTION_ENABLED
    override val preferenceTitle = "Retry Interval (second)"
}