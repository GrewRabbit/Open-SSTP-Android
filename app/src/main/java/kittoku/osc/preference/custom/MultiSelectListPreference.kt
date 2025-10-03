/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/MultiSelectListPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“多选列表”类型的偏好项（MultiSelectListPreference），如 SSL 加密套件和 PPP 认证协议的多选配置。
 * 主要功能：通过自定义 ModifiedMultiSelectListPreference 控件，展示和同步 SharedPreferences 中集合类型的配置项，便于用户在设置界面进行多项选择。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 SSLSuitesPreference、PPPAuthProtocolsPreference 等类，以实现多选功能。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、getSetPrefValue（集合读取工具）、initialize（偏好项初始化工具）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import kittoku.osc.preference.AUTH_PROTOCOL_EAP_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOL_MSCHAPv2
import kittoku.osc.preference.AUTH_PROTOCOl_PAP
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getSetPrefValue
import javax.net.ssl.SSLContext

/**
 * ModifiedMultiSelectListPreference
 * 多选列表偏好项基类，负责根据配置项同步选中值，并在附加到界面时初始化选项内容和摘要。
 */
internal abstract class ModifiedMultiSelectListPreference(context: Context, attrs: AttributeSet) : MultiSelectListPreference(context, attrs), OscPreference {
    // 可选值数组，子类需实现
    protected abstract val entryValues: Array<String>
    // 显示名称数组，可选，默认与 entryValues 相同
    protected open val entries: Array<String>? = null
    // 摘要提供器，可选，子类可自定义
    protected open val provider: SummaryProvider<Preference>? = null

    /**
     * 更新视图，将 SharedPreferences 中的集合值同步为当前选中项
     */
    override fun updateView() {
        values = getSetPrefValue(oscPrefKey, sharedPreferences!!)
    }

    /**
     * 当偏好项附加到界面时初始化选项内容和摘要
     */
    override fun onAttached() {
        setEntryValues(entryValues)
        setEntries(entries ?: entryValues)
        summaryProvider = provider
        initialize()
    }
}

/**
 * SSLSuitesPreference
 * 用于“SSL加密套件选择”配置项的多选列表偏好项，负责展示支持的加密套件。
 */
internal class SSLSuitesPreference(context: Context, attrs: AttributeSet) : ModifiedMultiSelectListPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_SUITES
    override val parentKey = OscPrefKey.SSL_DO_SELECT_SUITES
    override val preferenceTitle = "Select Cipher Suites"
    // 可选值为当前设备支持的所有 SSL 加密套件
    override val entryValues = SSLContext.getDefault().supportedSSLParameters.cipherSuites as Array<String>

    /**
     * 摘要提供器，根据已选项数量显示不同摘要
     */
    override val provider = SummaryProvider<Preference> {
        val currentValue = getSetPrefValue(oscPrefKey, it.sharedPreferences!!)
        when (currentValue.size) {
            0 -> "[No Suite Selected]"
            1 -> "1 Suite Selected"
            else -> "${currentValue.size} Suites Selected"
        }
    }
}

/**
 * PPPAuthProtocolsPreference
 * 用于“PPP认证协议选择”配置项的多选列表偏好项，负责展示支持的认证协议。
 */
internal class PPPAuthProtocolsPreference(context: Context, attrs: AttributeSet) : ModifiedMultiSelectListPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.PPP_AUTH_PROTOCOLS
    override val parentKey = null
    override val preferenceTitle = "Select Authentication Protocols"
    // 可选值为 PAP、MSCHAPv2、EAP-MSCHAPv2 三种协议
    override val entryValues = arrayOf(
        AUTH_PROTOCOl_PAP,
        AUTH_PROTOCOL_MSCHAPv2,
        AUTH_PROTOCOL_EAP_MSCHAPv2,
    )

    /**
     * 摘要提供器，根据已选项数量显示不同摘要
     */
    override val provider = SummaryProvider<Preference> {
        val currentValue = getSetPrefValue(oscPrefKey, it.sharedPreferences!!)
        when (currentValue.size) {
            0 -> "[No Protocol Selected]"
            1 -> "1 Protocol Selected"
            else -> "${currentValue.size} Protocols Selected"
        }
    }
}