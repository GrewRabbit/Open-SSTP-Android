/**
 * 文件：app/src/main/java/kittoku/osc/service/SstpTileService.kt
 * 作用说明：
 * 本文件实现了 Android 快速设置面板中的 SSTP VPN 开关磁贴服务（SstpTileService），
 * 允许用户通过磁贴一键连接或断开 SSTP VPN。
 * 主要功能：
 * - 根据 VPN 状态和配置合法性，动态更新磁贴显示状态（激活/未激活/不可用）。
 * - 响应用户点击磁贴，自动启动或停止 VPN 服务（SstpVpnService）。
 * - 监听 SharedPreferences 的 ROOT_STATE 变化，自动刷新磁贴状态。
 * 在本项目中，主界面、设置界面或系统磁贴管理会调用本文件（由系统自动注册 TileService）。
 * 本文件会调用 SstpVpnService（启动/停止 VPN）、kittoku.osc.preference.accessor 包下的 getBooleanPrefValue、
 * kittoku.osc.preference.checkPreferences（配置合法性校验）等工具方法。
 * 依赖 Android SDK 的 TileService、VpnService、SharedPreferences 组件。
 */

package kittoku.osc.service

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.checkPreferences

@RequiresApi(Build.VERSION_CODES.N)
internal class SstpTileService : TileService() {
    // 获取默认 SharedPreferences 实例
    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    // 监听 ROOT_STATE 配置项变化，自动刷新磁贴状态
    private val listener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == OscPrefKey.ROOT_STATE.name) {
                updateTileState()
            }
        }
    }

    // 获取当前 VPN 连接状态
    private val rootState: Boolean
        get() = getBooleanPrefValue(OscPrefKey.ROOT_STATE, prefs)

    // 判断 VPN 是否已准备好（权限已授权）
    private val isVpnPrepared: Boolean
        get() = VpnService.prepare(this) == null

    /**
     * 设置磁贴为不可用状态
     */
    private fun invalidateTileState() {
        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()
    }

    /**
     * 根据 VPN 状态刷新磁贴显示（激活/未激活）
     */
    private fun updateTileState() {
        qsTile.state = if (rootState) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    /**
     * 切换磁贴状态（激活/未激活）
     */
    private fun flipTileState() {
        qsTile.state = if (qsTile.state == Tile.STATE_ACTIVE) {
            Tile.STATE_INACTIVE
        } else {
            Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
    }

    /**
     * 初始化磁贴状态（根据 VPN 权限和连接状态）
     */
    private fun initializeState() {
        if (isVpnPrepared) {
            updateTileState()
        } else {
            invalidateTileState()
        }
    }

    /**
     * 磁贴首次添加时请求监听状态
     */
    override fun onTileAdded() {
        requestListeningState(this, ComponentName(this, SstpTileService::class.java))
    }

    /**
     * 开始监听磁贴时初始化状态并注册监听器
     */
    override fun onStartListening() {
        initializeState()
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 停止监听磁贴时注销监听器
     */
    override fun onStopListening() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 启动或停止 VPN 服务
     * @param action 连接或断开动作
     */
    private fun startVpnService(action: String) {
        val intent = Intent(this, SstpVpnService::class.java).setAction(action)
        if (action == ACTION_VPN_CONNECT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * 用户点击磁贴时触发，校验配置合法性并切换 VPN 状态
     */
    override fun onClick() {
        if (!isVpnPrepared || checkPreferences(prefs) != null) return

        flipTileState()

        when (qsTile.state) {
            Tile.STATE_ACTIVE -> startVpnService(ACTION_VPN_CONNECT)
            Tile.STATE_INACTIVE -> startVpnService(ACTION_VPN_DISCONNECT)
        }
    }
}