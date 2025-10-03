/**
 * 文件：app/src/main/java/kittoku/osc/preference/custom/common.kt
 * 作用说明：
 * 本文件定义了偏好项（Preference）相关的通用接口和抽象类，主要用于自定义和统一项目中的设置项视图及行为。
 * 主要功能包括：OscPreference接口用于规范所有自定义偏好项的属性和方法，initialize扩展函数用于初始化偏好项，OscEditTextPreference抽象类用于自定义文本输入偏好项。
 * 在本项目中，所有自定义偏好项（如复选框、文本输入等）会继承或实现本文件中的接口和类，偏好设置界面（如PreferenceFragment）会调用这些自定义偏好项以实现配置项的展示和交互。
 * 本文件会调用Android SDK的Preference相关组件、OscPrefKey（配置项枚举）、DEFAULT_INT_MAP/DEFAULT_STRING_MAP（默认值映射）、getIntPrefValue/getStringPrefValue（配置项读取工具）等模块。
 * 依赖Android SDK的Preference组件及项目内的配置项访问工具。
 */

package kittoku.osc.preference.custom

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import kittoku.osc.preference.DEFAULT_INT_MAP
import kittoku.osc.preference.DEFAULT_STRING_MAP
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue

/**
 * OscPreference接口
 * 规范所有自定义偏好项的属性和方法，便于统一管理和扩展。
 */
internal interface OscPreference {
    val oscPrefKey: OscPrefKey // 当前偏好项对应的配置项枚举
    val parentKey: OscPrefKey? // 父依赖项（用于界面依赖关系）
    val preferenceTitle: String // 偏好项标题
    fun updateView() // 用于同步视图与配置项数据
}

/**
 * initialize扩展函数
 * 用于初始化自定义偏好项，设置标题、依赖关系，并同步视图。
 */
internal fun <T> T.initialize() where T : Preference, T : OscPreference {
    title = preferenceTitle
    isSingleLineTitle = false

    parentKey?.also { dependency = it.name }

    updateView()
}

/**
 * OscEditTextPreference抽象类
 * 用于自定义文本输入偏好项，支持整型和字符串类型配置项的读取与展示。
 */
internal abstract class OscEditTextPreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs), OscPreference {
    protected open val inputType = InputType.TYPE_CLASS_TEXT // 输入类型（可重写）
    protected open val hint: String? = null // 输入框提示（可重写）
    protected open val provider: SummaryProvider<out Preference> = SimpleSummaryProvider.getInstance() // 摘要提供器

    /**
     * updateView方法
     * 根据配置项类型同步文本内容，支持整型和字符串类型。
     */
    override fun updateView() {
        text = when (oscPrefKey) {
            in DEFAULT_INT_MAP.keys -> getIntPrefValue(oscPrefKey, sharedPreferences!!).toString()
            in DEFAULT_STRING_MAP -> getStringPrefValue(oscPrefKey, sharedPreferences!!)
            else -> throw NotImplementedError(oscPrefKey.name)
        }
    }

    /**
     * onAttached方法
     * 当偏好项附加到界面时，设置输入框属性和摘要，并初始化偏好项。
     */
    override fun onAttached() {
        setOnBindEditTextListener {
            it.inputType = inputType
            it.hint = hint
            it.setSelection(it.text.length)
        }

        summaryProvider = provider

        initialize()
    }
}