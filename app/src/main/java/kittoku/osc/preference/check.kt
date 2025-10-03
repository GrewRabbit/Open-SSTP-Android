/**
 * 文件：app/src/main/java/kittoku/osc/preference/check.kt
 * 作用说明：
 * 本文件用于校验用户在偏好设置界面输入的各项参数是否合法，并在发现无效配置时进行提示。
 * 主要功能：
 * - toastInvalidSetting：弹窗提示无效设置信息。
 * - checkPreferences：对所有关键配置项进行合法性检查，返回首个发现的问题描述或 null（全部合法）。
 * 在本项目中，偏好设置相关模块（如 PreferenceFragment、设置保存逻辑等）会调用本文件的方法，以确保用户输入的参数有效，避免错误配置导致程序异常。
 * 本文件会调用 kittoku.osc.preference.accessor 包下的 getBooleanPrefValue、getIntPrefValue、getSetPrefValue、getStringPrefValue、getURIPrefValue 等工具方法。
 * 依赖 Android SDK 的 Toast、SharedPreferences 组件及项目内的常量定义（如 MAX_MRU、MIN_MTU 等）。
 */

package kittoku.osc.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import kittoku.osc.MAX_MRU
import kittoku.osc.MAX_MTU
import kittoku.osc.MIN_MRU
import kittoku.osc.MIN_MTU
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.accessor.getURIPrefValue

/**
 * 弹窗提示无效设置信息
 * @param message 错误信息
 * @param context 上下文环境
 */
internal fun toastInvalidSetting(message: String, context: Context) {
    Toast.makeText(context, "INVALID SETTING: $message", Toast.LENGTH_LONG).show()
}

/**
 * 校验所有关键配置项是否合法
 * @param prefs SharedPreferences 实例
 * @return 首个发现的问题描述，全部合法则返回 null
 */
internal fun checkPreferences(prefs: SharedPreferences): String? {
    // 校验主机名
    getStringPrefValue(OscPrefKey.HOME_HOSTNAME, prefs).also {
        if (it.isEmpty()) return "Hostname is missing"
    }

    // 校验 SSL 端口号
    getIntPrefValue(OscPrefKey.SSL_PORT, prefs).also {
        if (it !in 0..65535) return "The given port is out of 0-65535"
    }

    // 校验证书相关配置
    val doSpecifyCerts = getBooleanPrefValue(OscPrefKey.SSL_DO_SPECIFY_CERT, prefs)
    val version = getStringPrefValue(OscPrefKey.SSL_VERSION, prefs)
    val certDir = getURIPrefValue(OscPrefKey.SSL_CERT_DIR, prefs)
    if (doSpecifyCerts && version == "DEFAULT") return "Specifying trusted certificates needs SSL version to be specified"
    if (doSpecifyCerts && certDir == null) return "No certificates directory was selected"

    // 校验加密套件选择
    val doSelectSuites = getBooleanPrefValue(OscPrefKey.SSL_DO_SELECT_SUITES, prefs)
    val suites = getSetPrefValue(OscPrefKey.SSL_SUITES, prefs)
    if (doSelectSuites && suites.isEmpty()) return "No cipher suite was selected"

    // 校验自定义 SNI 配置
    val doUseCustomSNI = getBooleanPrefValue(OscPrefKey.SSL_DO_USE_CUSTOM_SNI, prefs)
    val isAPILevelLacked = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
    val customSNIHostname = getStringPrefValue(OscPrefKey.SSL_CUSTOM_SNI, prefs)
    if (doUseCustomSNI && isAPILevelLacked) return "Custom SNI needs 24 or higher API level"
    if (doUseCustomSNI && customSNIHostname.isEmpty()) return "Custom SNI Hostname must not be blank"

    // 校验代理相关配置
    if (getBooleanPrefValue(OscPrefKey.PROXY_DO_USE_PROXY, prefs)) {
        getStringPrefValue(OscPrefKey.HOME_HOSTNAME, prefs).also {
            if (it.isEmpty()) return "Proxy server hostname is missing"
        }
        getIntPrefValue(OscPrefKey.PROXY_PORT, prefs).also {
            if (it !in 0..65535) return "The given proxy server port is out of 0-65535"
        }
    }

    // 校验 PPP MRU/MTU 范围
    getIntPrefValue(OscPrefKey.PPP_MRU, prefs).also {
        if (it !in MIN_MRU..MAX_MRU) return "The given MRU is out of $MIN_MRU-$MAX_MRU"
    }
    getIntPrefValue(OscPrefKey.PPP_MTU, prefs).also {
        if (it !in MIN_MTU..MAX_MTU) return "The given MRU is out of $MIN_MTU-$MAX_MTU"
    }

    // 校验网络协议启用状态
    val isIPv4Enabled = getBooleanPrefValue(OscPrefKey.PPP_IPv4_ENABLED, prefs)
    val isIPv6Enabled = getBooleanPrefValue(OscPrefKey.PPP_IPv6_ENABLED, prefs)
    if (!isIPv4Enabled && !isIPv6Enabled) return "No network protocol was enabled"

    // 校验静态 IPv4 地址配置
    val isStaticIPv4Requested = getBooleanPrefValue(OscPrefKey.PPP_DO_REQUEST_STATIC_IPv4_ADDRESS, prefs)
    if (isIPv4Enabled && isStaticIPv4Requested) {
        getStringPrefValue(OscPrefKey.PPP_STATIC_IPv4_ADDRESS, prefs).also {
            if (it.isEmpty()) return "No static IPv4 address was given"
        }
    }

    // 校验认证协议选择
    val authProtocols = getSetPrefValue(OscPrefKey.PPP_AUTH_PROTOCOLS, prefs)
    if (authProtocols.isEmpty()) return "No authentication protocol was selected"

    // 校验认证超时时间
    getIntPrefValue(OscPrefKey.PPP_AUTH_TIMEOUT, prefs).also {
        if (it < 1) return "PPP authentication timeout period must be >=1 second"
    }

    // 校验自定义 DNS 服务器地址
    val isCustomDNSServerUsed = getBooleanPrefValue(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER, prefs)
    val isCustomAddressEmpty = getStringPrefValue(OscPrefKey.DNS_CUSTOM_ADDRESS, prefs).isEmpty()
    if (isCustomDNSServerUsed && isCustomAddressEmpty) {
        return "No custom DNS server address was given"
    }

    // 校验重连次数
    getIntPrefValue(OscPrefKey.RECONNECTION_COUNT, prefs).also {
        if (it < 1) return "Retry Count must be a positive integer"
    }

    // 校验日志保存目录
    val doSaveLog = getBooleanPrefValue(OscPrefKey.LOG_DO_SAVE_LOG, prefs)
    val logDir = getURIPrefValue(OscPrefKey.LOG_DIR, prefs)
    if (doSaveLog && logDir == null) return "No log directory was selected"

    // 全部合法
    return null
}