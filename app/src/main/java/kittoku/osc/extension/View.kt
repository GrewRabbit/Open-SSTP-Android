/**
 * 文件：app/src/main/java/kittoku/osc/extension/View.kt
 * 作用说明：
 * 本文件为 View 提供扩展方法，主要实现了递归查找第一个 EditText 子视图的工具函数 firstEditText。
 * 在本软件项目中，界面交互、表单处理、输入框定位等模块（如设置界面、表单校验、OscVpnService 等）会调用该文件，用于快速定位并操作界面中的 EditText 控件，提升 UI 处理的便利性。
 * 本文件依赖标准库 View、ViewGroup、EditText 及 androidx.core.view.children，不主动调用其他业务模块。
 */

package kittoku.osc.extension

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.children

/**
 * 递归查找当前 View 树中第一个 EditText 控件
 * @return 找到的 EditText 控件
 * @throws NotImplementedError 若未找到 EditText 时抛出异常
 */
internal fun View.firstEditText(): EditText {
    if (this is EditText) {
        return this
    }

    val viewGroups = mutableListOf<ViewGroup>()
    if (this is ViewGroup) {
        viewGroups.add(this)
    }

    while (true) {
        viewGroups.removeAt(0).children.forEach {
            if (it is EditText) {
                return it
            }

            if (it is ViewGroup) {
                viewGroups.add(it)
            }
        }

        if (viewGroups.isEmpty()) {
            throw NotImplementedError()
        }
    }
}