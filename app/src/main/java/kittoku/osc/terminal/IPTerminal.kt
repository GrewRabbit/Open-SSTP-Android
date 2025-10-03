/**
 * 文件：app/src/main/java/kittoku/osc/terminal/IPTerminal.kt
 * 作用说明：
 * 本文件实现了 VPN 隧道的 IP 层终端（IPTerminal），负责根据用户配置和 PPP 协议协商结果，
 * 初始化虚拟网络接口、设置路由、DNS、应用规则，并提供数据包的读写接口。
 * 主要功能：
 * - initialize：根据当前配置初始化虚拟网络接口，设置地址、路由、DNS、应用规则等。
 * - writePacket/readPacket：实现数据包的读写操作，供 VPN 主控流程调用。
 * - close：关闭虚拟网络接口，释放资源。
 * - setIPv4BasedRouting/setIPv6BasedRouting/addAppBasedRules/addCustomRoutes：辅助方法，分别用于设置 IPv4/IPv6 路由、应用规则、自定义路由。
 * 在本项目中，VPN 控制器（如 Controller 类）、主服务（SstpVpnService）会调用本文件的 IPTerminal，
 * 用于建立和管理 VPN 隧道的数据通道。
 * 本文件会调用 SharedBridge（桥接配置和状态）、kittoku.osc.preference.accessor 包下的配置项访问工具方法，
 * 以及 Android SDK 的 ParcelFileDescriptor、FileInputStream、FileOutputStream 等。
 */

package kittoku.osc.terminal

import android.os.ParcelFileDescriptor
import kittoku.osc.ControlMessage
import kittoku.osc.Result
import kittoku.osc.SharedBridge
import kittoku.osc.Where
import kittoku.osc.extension.toHexByteArray
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * IP 层终端类
 * 负责初始化虚拟网络接口、路由、DNS、应用规则，并提供数据包读写接口
 */
internal class IPTerminal(private val bridge: SharedBridge) {
    private var fd: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    // 配置项缓存
    private val isAppBasedRuleEnabled = bridge.allowedApps.isNotEmpty()
    private val isDefaultRouteAdded = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE, bridge.prefs)
    private val isPrivateAddressesRouted = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ROUTE_PRIVATE_ADDRESSES, bridge.prefs)
    private val isCustomDNSServerUsed = getBooleanPrefValue(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER, bridge.prefs)
    private val isCustomRoutesAdded = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES, bridge.prefs)

    /**
     * 初始化虚拟网络接口及相关路由、DNS、应用规则
     * 根据 PPP 协商结果和用户配置，设置 IPv4/IPv6 地址、DNS、路由等
     */
    internal suspend fun initialize() {
        if (bridge.PPP_IPv4_ENABLED) {
            if (bridge.currentIPv4.contentEquals(ByteArray(4))) {
                bridge.controlMailbox.send(ControlMessage(Where.IPv4, Result.ERR_INVALID_ADDRESS))
                return
            }

            InetAddress.getByAddress(bridge.currentIPv4).also {
                bridge.builder.addAddress(it, 32)
            }

            if (isCustomDNSServerUsed) {
                bridge.builder.addDnsServer(getStringPrefValue(OscPrefKey.DNS_CUSTOM_ADDRESS, bridge.prefs))
            }

            if (!bridge.currentProposedDNS.contentEquals(ByteArray(4))) {
                InetAddress.getByAddress(bridge.currentProposedDNS).also {
                    bridge.builder.addDnsServer(it)
                }
            }

            setIPv4BasedRouting()
        }

        if (bridge.PPP_IPv6_ENABLED) {
            if (bridge.currentIPv6.contentEquals(ByteArray(8))) {
                bridge.controlMailbox.send(ControlMessage(Where.IPv6, Result.ERR_INVALID_ADDRESS))
                return
            }

            ByteArray(16).also { // 构造 link-local 地址
                "FE80".toHexByteArray().copyInto(it)
                ByteArray(6).copyInto(it, destinationOffset = 2)
                bridge.currentIPv6.copyInto(it, destinationOffset = 8)
                bridge.builder.addAddress(InetAddress.getByAddress(it), 64)
            }

            setIPv6BasedRouting()
        }

        if (isCustomRoutesAdded) {
            addCustomRoutes()
        }

        if (isAppBasedRuleEnabled) {
            addAppBasedRules()
        }

        bridge.builder.setMtu(bridge.PPP_MTU)
        bridge.builder.setBlocking(true)

        fd = bridge.builder.establish()!!.also {
            inputStream = FileInputStream(it.fileDescriptor)
            outputStream = FileOutputStream(it.fileDescriptor)
        }

        bridge.controlMailbox.send(ControlMessage(Where.IP, Result.PROCEEDED))
    }

    /**
     * 设置 IPv4 路由（默认路由、私有地址路由）
     */
    private fun setIPv4BasedRouting() {
        if (isDefaultRouteAdded) {
            bridge.builder.addRoute("0.0.0.0", 0)
        }

        if (isPrivateAddressesRouted) {
            bridge.builder.addRoute("10.0.0.0", 8)
            bridge.builder.addRoute("172.16.0.0", 12)
            bridge.builder.addRoute("192.168.0.0", 16)
        }
    }

    /**
     * 设置 IPv6 路由（默认路由、私有地址路由）
     */
    private fun setIPv6BasedRouting() {
        if (isDefaultRouteAdded) {
            bridge.builder.addRoute("::", 0)
        }

        if (isPrivateAddressesRouted) {
            bridge.builder.addRoute("fc00::", 7)
        }
    }

    /**
     * 添加基于应用的访问规则
     */
    private fun addAppBasedRules() {
        bridge.allowedApps.forEach {
            bridge.builder.addAllowedApplication(it.packageName)
        }
    }

    /**
     * 添加自定义路由
     * @return 是否全部添加成功
     */
    private suspend fun addCustomRoutes(): Boolean {
        getStringPrefValue(OscPrefKey.ROUTE_CUSTOM_ROUTES, bridge.prefs).split("\n").filter { it.isNotEmpty() }.forEach {
            val parsed = it.split("/")
            if (parsed.size != 2) {
                bridge.controlMailbox.send(ControlMessage(Where.ROUTE, Result.ERR_PARSING_FAILED))
                return false
            }

            val address = parsed[0]
            val prefix = parsed[1].toIntOrNull()
            if (prefix == null){
                bridge.controlMailbox.send(ControlMessage(Where.ROUTE, Result.ERR_PARSING_FAILED))
                return false
            }

            try {
                bridge.builder.addRoute(address, prefix)
            } catch (_: IllegalArgumentException) {
                bridge.controlMailbox.send(ControlMessage(Where.ROUTE, Result.ERR_PARSING_FAILED))
                return false
            }
        }

        return true
    }

    /**
     * 写入数据包到虚拟网络接口
     * @param start 起始位置
     * @param size 数据长度
     * @param buffer 数据缓冲区
     */
    internal fun writePacket(start: Int, size: Int, buffer: ByteBuffer) {
        // 未初始化时不写入
        outputStream?.write(buffer.array(), start, size)
    }

    /**
     * 从虚拟网络接口读取数据包
     * @param buffer 数据缓冲区
     */
    internal fun readPacket(buffer: ByteBuffer) {
        buffer.clear()
        buffer.position(inputStream?.read(buffer.array(), 0, bridge.PPP_MTU) ?: buffer.position())
        buffer.flip()
    }

    /**
     * 关闭虚拟网络接口，释放资源
     */
    internal fun close() {
        fd?.close()
    }
}