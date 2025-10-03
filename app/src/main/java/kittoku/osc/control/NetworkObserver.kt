/**
 * 文件：app/src/main/java/kittoku/osc/control/NetworkObserver.kt
 * 作用说明：
 * 本文件实现了 NetworkObserver 网络状态观察器，用于监控 VPN 网络连接的变化，收集并更新当前 VPN 链路的状态信息（如 IP、DNS、路由、加密参数等）。
 * 在本软件项目中，Controller（主流程控制器）会实例化和调用该文件，用于在 VPN 连接建立后持续更新连接状态，便于前端展示和问题排查。
 * 本文件会调用 SharedBridge（桥接数据结构）、setStringPrefValue（偏好设置写入工具），并依赖 Android 的 ConnectivityManager、NetworkCallback 等系统 API。
 * NetworkObserver 负责注册网络回调，监听 VPN 网络的可用性和属性变化，自动更新状态摘要到偏好设置，支持关闭和资源释放。
 */

package kittoku.osc.control

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kittoku.osc.SharedBridge
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.setStringPrefValue

// 网络状态观察器，负责监听 VPN 网络变化并更新状态摘要
internal class NetworkObserver(val bridge: SharedBridge) {
    // 系统网络管理器
    private val manager = bridge.service.getSystemService(ConnectivityManager::class.java)
    // 网络回调对象
    private val callback: ConnectivityManager.NetworkCallback

    // 构造方法，初始化回调并注册监听
    init {
        wipeStatus() // 清空状态摘要

        // 构建 VPN 网络请求
        val request = NetworkRequest.Builder().let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            it.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            it.build()
        }

        // 定义网络回调，监听网络可用和属性变化
        callback = object : ConnectivityManager.NetworkCallback() {
            // 网络可用时回调（Android O 以下需手动获取属性）
            override fun onAvailable(network: Network) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    manager.getLinkProperties(network)?.also {
                        updateSummary(it)
                    }
                }
            }

            // 网络属性变化时回调，更新状态摘要
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                updateSummary(linkProperties)
            }
        }

        // 注册网络回调
        manager.registerNetworkCallback(request, callback)
    }

    // 更新 VPN 状态摘要到偏好设置
    private fun updateSummary(properties: LinkProperties) {
        val summary = mutableListOf<String>()

        // 添加 SSL/TLS 加密参数
        bridge.sslTerminal!!.getSession().also {
            if (!it.isValid) return

            summary.add("[SSL/TLS Parameters]")
            summary.add("PROTOCOL: ${it.protocol}")
            summary.add("SUITE: ${it.cipherSuite}")
        }
        summary.add("")

        // 添加分配的 IP 地址
        summary.add("[Assigned IP Address]")
        properties.linkAddresses.forEach {
            summary.add(it.address.hostAddress ?: "")
        }
        summary.add("")

        // 添加 DNS 服务器地址
        summary.add("[DNS Server Address]")
        if (properties.dnsServers.isNotEmpty()) {
            properties.dnsServers.forEach {
                summary.add(it.hostAddress ?: "")
            }
        } else {
            summary.add("Not specified")
        }
        summary.add("")

        // 添加路由信息
        summary.add("[Routing]")
        properties.routes.forEach {
            summary.add(it.toString())
        }
        summary.add("")

        // 添加允许的应用列表
        summary.add("[Allowed Apps]")
        if (bridge.allowedApps.isNotEmpty()) {
            bridge.allowedApps.forEach { summary.add(it.label) }
        } else {
            summary.add("All apps")
        }

        // 合并所有信息并写入偏好设置
        summary.reduce { acc, s ->
            acc + "\n" + s
        }.also {
            setStringPrefValue(it, OscPrefKey.HOME_STATUS, bridge.prefs)
        }
    }

    // 清空 VPN 状态摘要
    private fun wipeStatus() {
        setStringPrefValue("", OscPrefKey.HOME_STATUS, bridge.prefs)
    }

    // 关闭观察器，注销回调并清空状态
    internal fun close() {
        try {
            manager.unregisterNetworkCallback(callback)
        } catch (_: IllegalArgumentException) {} // 已注销无需处理

        wipeStatus()
    }
}