/**
 * BlankActivity.kt 作用说明：
 * 本文件是一个通用的 Activity，用于根据传入参数动态加载不同的 Fragment（Profiles、Allowed Apps、保存证书界面）。
 * 通过 Intent 跳转并传递参数，其他 Activity 或 Fragment 可调用本文件以展示对应界面。
 * 本文件会根据参数分别调用 ProfilesFragment、AppsFragment、SaveCertFragment，负责实际的界面和业务逻辑展示。
 * 作为 Fragment 的统一承载容器，简化了界面跳转和管理，提升了代码复用性。
 */

package kittoku.osc.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kittoku.osc.R
import kittoku.osc.databinding.ActivityBlankBinding
import kittoku.osc.fragment.AppsFragment
import kittoku.osc.fragment.ProfilesFragment
import kittoku.osc.fragment.SaveCertFragment

// 定义不同类型的常量，用于区分要显示的 Fragment
internal const val BLANK_ACTIVITY_TYPE_PROFILES = 0
internal const val BLANK_ACTIVITY_TYPE_APPS = 1
internal const val BLANK_ACTIVITY_TYPE_SAVE_CERT = 2

// Intent 传递参数的 Key
internal const val EXTRA_KEY_TYPE = "TYPE"
internal const val EXTRA_KEY_CERT = "CERT"
internal const val EXTRA_KEY_FILENAME = "FILENAME"

// 空白活动类，根据传入的类型参数加载不同的 Fragment
class BlankActivity : AppCompatActivity() {
    // 生命周期方法，活动创建时调用
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment: Fragment

        // 根据 Intent 传递的类型参数，选择要显示的 Fragment
        when (intent.extras!!.getInt(EXTRA_KEY_TYPE)) {
            BLANK_ACTIVITY_TYPE_PROFILES -> {
                title = "Profiles" // 设置标题
                fragment = ProfilesFragment() // 加载 ProfilesFragment
            }

            BLANK_ACTIVITY_TYPE_APPS -> {
                title = "Allowed Apps" // 设置标题
                fragment = AppsFragment() // 加载 AppsFragment
            }

            BLANK_ACTIVITY_TYPE_SAVE_CERT -> {
                fragment = SaveCertFragment(intent) // 加载 SaveCertFragment，并传递 intent
            }

            else -> throw NotImplementedError(intent.toString()) // 未知类型抛出异常
        }

        // 使用 ViewBinding 加载布局
        val binding = ActivityBlankBinding.inflate(layoutInflater)
        binding.root.fitsSystemWindows = true
        setContentView(binding.root)

        // 将选中的 Fragment 替换到布局中
        supportFragmentManager.beginTransaction().also {
            it.replace(R.id.blank, fragment)
            it.commit()
        }
    }
}
