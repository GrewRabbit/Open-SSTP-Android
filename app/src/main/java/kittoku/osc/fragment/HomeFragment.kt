/**
 * 文件：app/src/main/java/kittoku/osc/fragment/HomeFragment.kt
 * 作用说明：
 * 本文件定义了 HomeFragment，用于主界面管理 VPN 连接的开关操作，负责处理用户点击连接/断开按钮的逻辑。
 * 主要功能包括：监听连接器控件状态变化、校验用户设置、发起 VPN 连接或断开服务、处理 VPN 权限申请流程。
 * 在本软件项目中，主界面相关模块会调用该文件，用于实现 VPN 的启动与关闭操作。
 * 本文件会调用 kittoku.osc.preference.checkPreferences（校验设置）、kittoku.osc.preference.toastInvalidSetting（提示无效设置）、kittoku.osc.preference.custom.HomeConnectorPreference（连接器控件）、kittoku.osc.service.SstpVpnService（VPN 服务）等模块。
 * 本文件依赖标准库 Activity、Intent、VpnService 及 AndroidX Preference 库。
 */

package kittoku.osc.fragment

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kittoku.osc.R
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.checkPreferences
import kittoku.osc.preference.custom.HomeConnectorPreference
import kittoku.osc.preference.toastInvalidSetting
import kittoku.osc.service.ACTION_VPN_CONNECT
import kittoku.osc.service.ACTION_VPN_DISCONNECT
import kittoku.osc.service.SstpVpnService

/**
 * 主界面 VPN 连接管理 Fragment
 */
class HomeFragment : PreferenceFragmentCompat() {
    // 处理 VPN 权限申请结果的 Launcher
    private val preparationLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(ACTION_VPN_CONNECT)
        }
    }

    /**
     * 加载主界面偏好设置布局
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home, rootKey)
    }

    /**
     * 界面创建后，绑定连接器控件监听器
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachConnectorListener()
    }

    /**
     * 启动或停止 VPN 服务
     * @param action 服务操作类型（连接/断开）
     */
    private fun startVpnService(action: String) {
        val intent = Intent(requireContext(), SstpVpnService::class.java).setAction(action)
        if (action == ACTION_VPN_CONNECT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    /**
     * 绑定连接器控件的状态变化监听器
     */
    private fun attachConnectorListener() {
        findPreference<HomeConnectorPreference>(OscPrefKey.HOME_CONNECTOR.name)!!.also {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newState ->
                if (newState == true) {
                    // 校验设置，若无效则提示并阻止连接
                    checkPreferences(preferenceManager.sharedPreferences!!)?.also { message ->
                        toastInvalidSetting(message, requireContext())
                        return@OnPreferenceChangeListener false
                    }
                    // 申请 VPN 权限，成功后启动服务
                    VpnService.prepare(requireContext())?.also { intent ->
                        preparationLauncher.launch(intent)
                    } ?: startVpnService(ACTION_VPN_CONNECT)
                } else {
                    // 断开 VPN 服务
                    startVpnService(ACTION_VPN_DISCONNECT)
                }
                true
            }
        }
    }
}