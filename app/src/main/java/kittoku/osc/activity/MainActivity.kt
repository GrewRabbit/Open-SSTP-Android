/*
 * MainActivity 主要功能及调用关系说明（思维导图结构）：
 * MainActivity
 * ├─ 生命周期入口
 * │  └─ onCreate
 * │     ├─ 初始化视图（ActivityMainBinding）
 * │     ├─ 初始化 SharedPreferences
 * │     ├─ 创建 HomeFragment、SettingFragment
 * │     ├─ 配置 FragmentStateAdapter
 * │     └─ 配置 TabLayoutMediator
 * ├─ 菜单相关
 * │  ├─ onCreateOptionsMenu
 * │  └─ onOptionsItemSelected
 * │     ├─ R.id.load_profile → 启动 BlankActivity（加载 ProfilesFragment）
 * │     ├─ R.id.save_profile → showSaveDialog
 * │     └─ R.id.reload_defaults → showReloadDialog
 * ├─ 配置相关
 * │  ├─ showSaveDialog
 * │  │  └─ 保存配置到 SharedPreferences
 * │  └─ showReloadDialog
 * │     └─ 调用 importProfile 恢复默认配置
 * ├─ 界面刷新
 * │  └─ updatePreferenceView
 * │     └─ 遍历 Fragment，刷新 OscPreference 视图
 */

package kittoku.osc.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.forEach
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kittoku.osc.BuildConfig
import kittoku.osc.R
import kittoku.osc.databinding.ActivityMainBinding
import kittoku.osc.extension.firstEditText
import kittoku.osc.extension.sum
import kittoku.osc.fragment.HomeFragment
import kittoku.osc.fragment.SettingFragment
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.PROFILE_KEY_HEADER
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.custom.OscPreference
import kittoku.osc.preference.exportProfile
import kittoku.osc.preference.importProfile

// 主界面 Activity，负责应用主入口、配置管理、界面切换
class MainActivity : AppCompatActivity() {
    // SharedPreferences 用于存储和读取配置
    private lateinit var prefs: SharedPreferences

    // 主界面和设置界面 Fragment
    private lateinit var homeFragment: PreferenceFragmentCompat
    private lateinit var settingFragment: PreferenceFragmentCompat

    // 用于弹窗输入的布局资源
    private val dialogResource: Int by lazy { EditTextPreference(this).dialogLayoutResource }

    // 启动 BlankActivity 用于加载配置文件
    private val profileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }
        // 配置变更后刷新界面
        updatePreferenceView()
    }

    // 刷新界面所有 OscPreference 视图
    private fun updatePreferenceView() {
        listOf(homeFragment, settingFragment).forEach { fragment ->
            if (fragment.isAdded) {
                val preferenceGroups = mutableListOf<PreferenceGroup>(fragment.preferenceScreen)
                while (preferenceGroups.isNotEmpty()) {
                    preferenceGroups.removeAt(0).forEach {
                        if (it is OscPreference) {
                            it.updateView()
                        }
                        if (it is PreferenceGroup) {
                            preferenceGroups.add(it)
                        }
                    }
                }
            }
        }
    }

    // 生命周期入口，初始化主界面和各功能模块
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "${getString(R.string.app_name)}: ${BuildConfig.VERSION_NAME}"
        val binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.fitsSystemWindows = true
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        homeFragment = HomeFragment()
        settingFragment = SettingFragment()

        // 配置 FragmentStateAdapter，实现主界面和设置界面切换
        object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> homeFragment
                    1 -> settingFragment
                    else -> throw NotImplementedError(position.toString())
                }
            }
        }.also {
            binding.pager.adapter = it
        }

        // 配置 TabLayoutMediator，实现标签栏切换
        TabLayoutMediator(binding.tabBar, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Home"
                1 -> "设置"
                else -> throw NotImplementedError(position.toString())
            }
        }.attach()
    }

    // 创建菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.home_menu, menu)
        return true
    }

    // 菜单项点击事件，分别处理加载配置、保存配置、恢复默认
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.load_profile -> {
                profileLauncher.launch(Intent(this, BlankActivity::class.java).putExtra(
                    EXTRA_KEY_TYPE,
                    BLANK_ACTIVITY_TYPE_PROFILES
                ))
            }
            R.id.save_profile -> showSaveDialog()
            R.id.reload_defaults -> showReloadDialog()
        }
        return true
    }

    // 保存配置弹窗，输入配置名并保存到 SharedPreferences
    private fun showSaveDialog() {
        val inflated = layoutInflater.inflate(dialogResource, null)
        val editText = inflated.firstEditText()
        val hostname = getStringPrefValue(OscPrefKey.HOME_HOSTNAME, prefs)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.hint = hostname
        editText.requestFocus()

        AlertDialog.Builder(this).also {
            it.setView(inflated)
            it.setMessage(sum(
                "请输入配置名称。\n",
                "如果留空，将使用主机名。\n",
                "如果名称重复，将覆盖原有配置。"
            ))
            it.setPositiveButton("保存") { _, _ ->
                prefs.edit().also { editor ->
                    editor.putString(
                        PROFILE_KEY_HEADER + editText.text.ifEmpty { hostname },
                        exportProfile(prefs)
                    )
                    editor.apply()
                }
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
            }
            it.setNegativeButton("取消") { _, _ -> }
            it.show()
        }
    }

    // 恢复默认配置弹窗，确认后调用 importProfile
    private fun showReloadDialog() {
        AlertDialog.Builder(this).also {
            it.setMessage("确定要恢复默认设置吗？")
            it.setPositiveButton("确定") { _, _ ->
                importProfile(null, prefs)
                updatePreferenceView()
                Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show()
            }
            it.setNegativeButton("取消") { _, _ -> }
            it.show()
        }
    }
}