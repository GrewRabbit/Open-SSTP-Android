/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/DirectoryPreference.kt
 * 作用说明：
 * 本文件用于定义项目中“目录选择”类型的偏好项（Preference），如证书目录和日志目录的选择。
 * 主要功能：通过自定义 Preference 控件，展示和同步 SharedPreferences 中 URI 类型的目录路径，便于用户在设置界面选择和显示相关目录。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 DirectoryPreference 及其子类，以实现目录选择功能。
 * 本文件会调用 SharedPreferences（Android 标准库）、OscPrefKey（配置项枚举）、getURIPrefValue（URI 读取工具）、initialize（偏好项初始化工具）等模块。
 * 依赖 Android SDK 的 Preference 组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getURIPrefValue

/**
 * DirectoryPreference
 * 目录选择偏好项基类，负责根据配置项同步目录路径到界面摘要。
 */
internal abstract class DirectoryPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs), OscPreference {
    /**
     * 更新视图，将 SharedPreferences 中的 URI 路径显示为摘要
     */
    override fun updateView() {
        summary = getURIPrefValue(oscPrefKey, sharedPreferences!!)?.path ?: "[No Directory Selected]"
    }

    /**
     * 当偏好项附加到界面时初始化
     */
    override fun onAttached() {
        initialize()
    }
}

/**
 * SSLCertDirPreference
 * 用于“选择受信任证书目录”配置项的目录选择偏好，依赖于 SSL_DO_SPECIFY_CERT 配置项。
 */
internal class SSLCertDirPreference(context: Context, attrs: AttributeSet) : DirectoryPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.SSL_CERT_DIR
    override val preferenceTitle = "Select Trusted Certificates"
    override val parentKey = OscPrefKey.SSL_DO_SPECIFY_CERT
}

/**
 * LogDirPreference
 * 用于“选择日志目录”配置项的目录选择偏好，依赖于 LOG_DO_SAVE_LOG 配置项。
 */
internal class LogDirPreference(context: Context, attrs: AttributeSet) : DirectoryPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.LOG_DIR
    override val preferenceTitle = "Select Log Directory"
    override val parentKey = OscPrefKey.LOG_DO_SAVE_LOG
}