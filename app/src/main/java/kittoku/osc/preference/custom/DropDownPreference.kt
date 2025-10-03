/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/DropDownPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“下拉选择”类型的偏好项（DropDownPreference），如 SSL 版本选择等。
 * 主要功能：通过自定义 DropDownPreference 控件，展示和同步 SharedPreferences 中字符串类型的配置项，便于用户在设置界面进行选项选择。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 ModifiedDropDownPreference 及其子类，以实现下拉选择功能。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、getStringPrefValue（字符串读取工具）、initialize（偏好项初始化工具）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DropDownPreference
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getStringPrefValue
import javax.net.ssl.SSLContext

/**
 * ModifiedDropDownPreference
 * 自定义下拉选择偏好项基类，扩展了 DropDownPreference 并实现 OscPreference 接口。
 * 负责根据配置项同步选中值，并在附加到界面时初始化选项内容。
 */
internal abstract class ModifiedDropDownPreference(context: Context, attrs: AttributeSet) : DropDownPreference(context, attrs), OscPreference {
    // 可选值数组，子类需实现
    protected abstract val values: Array<String>
    // 显示名称数组，可选，默认与 values 相同
    protected open val names: Array<String>? = null

    /**
     * 更新视图，将 SharedPreferences 中的字符串值同步为当前选中项
     */
    override fun updateView() {
        value = getStringPrefValue(oscPrefKey, sharedPreferences!!)
    }

    /**
     * 当偏好项附加到界面时初始化选项内容和摘要
     */
    override fun onAttached() {
        entryValues = values
        entries = names ?: values
        summaryProvider = SimpleSummaryProvider.getInstance()

        initialize()
    }
}

/**
 * SSLVersionPreference
 * 用于“SSL 版本选择”配置项的下拉选择偏好，负责展示支持的 SSL 协议版本。
 */
internal class SSLVersionPreference(context: Context, attrs: AttributeSet) : ModifiedDropDownPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_VERSION
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "SSL Version"
    // 可选值为 DEFAULT 及当前设备支持的所有 SSL 协议
    override val values = arrayOf("DEFAULT") + SSLContext.getDefault().supportedSSLParameters.protocols
}