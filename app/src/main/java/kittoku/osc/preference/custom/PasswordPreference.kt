/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/PasswordPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“密码输入”类型的偏好项（PasswordPreference），如主密码和代理密码的输入控件。
 * 主要功能：通过自定义 PasswordPreference 控件，展示和同步 SharedPreferences 中的密码字段，输入内容以密文形式显示，摘要仅提示是否已输入密码。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 HomePasswordPreference、ProxyPasswordPreference，以实现密码输入功能。
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
 * PasswordPreference
 * 密码输入偏好项基类，继承自 OscEditTextPreference，强制输入类型为密码，摘要仅提示是否已输入密码。
 */
internal abstract class PasswordPreference(context: Context, attrs: AttributeSet) : OscEditTextPreference(context, attrs) {
    // 设置输入框类型为密码
    override val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    /**
     * 摘要提供器，根据是否输入密码显示不同摘要
     */
    override val provider = SummaryProvider<Preference> {
        val currentValue = getStringPrefValue(oscPrefKey, it.sharedPreferences!!)
        if (currentValue.isEmpty()) {
            "[No Value Entered]"
        } else {
            "[Password Entered]"
        }
    }
}

/**
 * HomePasswordPreference
 * 用于“主密码”配置项的密码输入偏好项。
 */
internal class HomePasswordPreference(context: Context, attrs: AttributeSet) : PasswordPreference(context, attrs) {
    // 对应配置项枚举
    override val oscPrefKey = OscPrefKey.HOME_PASSWORD
    // 无父依赖项
    override val parentKey: OscPrefKey? = null
    // 偏好项标题
    override val preferenceTitle = "Password"
}

/**
 * ProxyPasswordPreference
 * 用于“代理密码”配置项的密码输入偏好项，依赖于是否启用代理。
 */
internal class ProxyPasswordPreference(context: Context, attrs: AttributeSet) : PasswordPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PROXY_PASSWORD
    // 依赖于 PROXY_DO_USE_PROXY 配置项
    override val parentKey = OscPrefKey.PROXY_DO_USE_PROXY
    override val preferenceTitle = "Proxy Password (optional)"
}