/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/LinkPreference.kt
 * 作用说明：
 * 本文件用于定义“外部链接”类型的偏好项（LinkPreference），在设置界面中以点击跳转的形式展示外部网页链接（如项目主页）。
 * 主要功能：通过自定义 Preference 控件，展示链接标题和摘要，点击后自动跳转到指定 URL。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、自定义设置页面等）会调用本文件中的 LinkPreference 及其子类，以实现外部跳转功能。
 * 本文件会调用 Android SDK 的 Intent、Preference 组件及 androidx.core.net.toUri 等工具。
 * 依赖 Android SDK 的 Preference 组件及 Intent 跳转机制。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.core.net.toUri

/**
 * LinkPreference
 * 外部链接偏好项基类，负责展示链接标题、摘要，并实现点击跳转到指定 URL。
 */
internal abstract class LinkPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    // 链接标题
    abstract val preferenceTitle: String
    // 链接摘要
    abstract val preferenceSummary: String
    // 跳转的 URL
    abstract val url: String

    /**
     * 当偏好项附加到界面时，设置标题和摘要
     */
    override fun onAttached() {
        super.onAttached()
        title = preferenceTitle
        summary = preferenceSummary
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

    /**
     * 点击偏好项时，跳转到指定 URL
     */
    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW).also { it.data = url.toUri() }
        intent.resolveActivity(context.packageManager)?.also {
            context.startActivity(intent)
        }
    }
}

/**
 * LinkOscPreference
 * 用于跳转到本项目 GitHub 主页的链接偏好项。
 */
internal class LinkOscPreference(context: Context, attrs: AttributeSet) : LinkPreference(context, attrs) {
    override val preferenceTitle = "Move to this app's project page"
    override val preferenceSummary = "github.com/kittoku/Open-SSTP-Client"
    override val url = "https://github.com/kittoku/Open-SSTP-Client"
}