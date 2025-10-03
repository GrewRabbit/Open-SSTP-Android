/**
 * 文件：app/src/main/java/kittoku/osc/preference/constant.kt
 * 作用说明：
 * 本文件用于定义项目中所有偏好设置项（OscPrefKey 枚举）及其默认值（布尔型、整型、字符串、集合、Uri），
 * 主要为 SharedPreferences 的初始化和访问提供统一的键值和默认值映射。
 * 主要功能：
 * - OscPrefKey：枚举所有配置项的键名，供偏好设置相关模块统一引用。
 * - DEFAULT_BOOLEAN_MAP/DEFAULT_INT_MAP/DEFAULT_STRING_MAP/DEFAULT_SET_MAP/DEFAULT_URI_MAP：定义各类型配置项的默认值，便于初始化和重置。
 * - AUTH_PROTOCOL 常量：定义支持的认证协议名称。
 * - TEMP_KEY_HEADER/PROFILE_KEY_HEADER：定义临时和配置文件相关的键名前缀。
 * 在本项目中，偏好设置相关模块（如 accessor 工具、各类 Preference 控件、配置校验 check.kt、应用管理 app.kt 等）会调用本文件的枚举和默认值映射，
 * 以实现配置项的统一管理和访问。
 * 本文件不直接调用其他文件，但依赖 kittoku.osc 包下的 DEFAULT_MRU、DEFAULT_MTU 常量。
 * 依赖 Android SDK 的 Uri 类型。
 */

package kittoku.osc.preference

import android.net.Uri
import kittoku.osc.DEFAULT_MRU
import kittoku.osc.DEFAULT_MTU

/**
 * OscPrefKey
 * 枚举所有偏好设置项的键名，供 SharedPreferences 及各类控件统一引用。
 */
internal enum class OscPrefKey {
    ROOT_STATE,
    HOME_HOSTNAME,
    HOME_USERNAME,
    HOME_PASSWORD,
    HOME_CONNECTOR,
    HOME_STATUS,
    SSL_PORT,
    SSL_VERSION,
    SSL_DO_VERIFY,
    SSL_DO_SPECIFY_CERT,
    SSL_CERT_DIR,
    SSL_DO_SELECT_SUITES,
    SSL_SUITES,
    SSL_DO_USE_CUSTOM_SNI,
    SSL_CUSTOM_SNI,
    PROXY_DO_USE_PROXY,
    PROXY_HOSTNAME,
    PROXY_PORT,
    PROXY_USERNAME,
    PROXY_PASSWORD,
    PPP_MRU,
    PPP_MTU,
    PPP_AUTH_PROTOCOLS,
    PPP_AUTH_TIMEOUT,
    PPP_IPv4_ENABLED,
    PPP_DO_REQUEST_STATIC_IPv4_ADDRESS,
    PPP_STATIC_IPv4_ADDRESS,
    PPP_IPv6_ENABLED,
    DNS_DO_REQUEST_ADDRESS,
    DNS_DO_USE_CUSTOM_SERVER,
    DNS_CUSTOM_ADDRESS,
    ROUTE_DO_ADD_DEFAULT_ROUTE,
    ROUTE_DO_ROUTE_PRIVATE_ADDRESSES,
    ROUTE_DO_ADD_CUSTOM_ROUTES,
    ROUTE_CUSTOM_ROUTES,
    ROUTE_DO_ENABLE_APP_BASED_RULE,
    ROUTE_ALLOWED_APPS,
    RECONNECTION_ENABLED,
    RECONNECTION_COUNT,
    RECONNECTION_INTERVAL,
    RECONNECTION_LIFE,
    LOG_DO_SAVE_LOG,
    LOG_DIR,
}

/**
 * DEFAULT_BOOLEAN_MAP
 * 定义所有布尔型配置项的默认值。
 */
internal val DEFAULT_BOOLEAN_MAP = mapOf(
    OscPrefKey.ROOT_STATE to false,
    OscPrefKey.HOME_CONNECTOR to false,
    OscPrefKey.SSL_DO_VERIFY to true,
    OscPrefKey.SSL_DO_SPECIFY_CERT to false,
    OscPrefKey.SSL_DO_SELECT_SUITES to false,
    OscPrefKey.SSL_DO_USE_CUSTOM_SNI to false,
    OscPrefKey.PROXY_DO_USE_PROXY to false,
    OscPrefKey.PPP_IPv4_ENABLED to true,
    OscPrefKey.PPP_DO_REQUEST_STATIC_IPv4_ADDRESS to false,
    OscPrefKey.PPP_IPv6_ENABLED to false,
    OscPrefKey.DNS_DO_REQUEST_ADDRESS to true,
    OscPrefKey.DNS_DO_USE_CUSTOM_SERVER to false,
    OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE to true,
    OscPrefKey.ROUTE_DO_ROUTE_PRIVATE_ADDRESSES to false,
    OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES to false,
    OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE to false,
    OscPrefKey.RECONNECTION_ENABLED to false,
    OscPrefKey.LOG_DO_SAVE_LOG to false
)

/**
 * DEFAULT_INT_MAP
 * 定义所有整型配置项的默认值。
 */
internal val DEFAULT_INT_MAP = mapOf(
    OscPrefKey.SSL_PORT to 443,
    OscPrefKey.PROXY_PORT to 8080,
    OscPrefKey.PPP_MRU to DEFAULT_MRU,
    OscPrefKey.PPP_MTU to DEFAULT_MTU,
    OscPrefKey.PPP_AUTH_TIMEOUT to 3,
    OscPrefKey.RECONNECTION_COUNT to 3,
    OscPrefKey.RECONNECTION_INTERVAL to 10,
    OscPrefKey.RECONNECTION_LIFE to 0
)

private const val EMPTY_TEXT = ""

/**
 * DEFAULT_STRING_MAP
 * 定义所有字符串型配置项的默认值。
 */
internal val DEFAULT_STRING_MAP = mapOf(
    OscPrefKey.HOME_HOSTNAME to EMPTY_TEXT,
    OscPrefKey.HOME_USERNAME to EMPTY_TEXT,
    OscPrefKey.HOME_PASSWORD to EMPTY_TEXT,
    OscPrefKey.HOME_STATUS to EMPTY_TEXT,
    OscPrefKey.SSL_CUSTOM_SNI to EMPTY_TEXT,
    OscPrefKey.PROXY_HOSTNAME to EMPTY_TEXT,
    OscPrefKey.PROXY_USERNAME to EMPTY_TEXT,
    OscPrefKey.PROXY_PASSWORD to EMPTY_TEXT,
    OscPrefKey.PPP_STATIC_IPv4_ADDRESS to EMPTY_TEXT,
    OscPrefKey.DNS_CUSTOM_ADDRESS to EMPTY_TEXT,
    OscPrefKey.ROUTE_CUSTOM_ROUTES to EMPTY_TEXT,
    OscPrefKey.SSL_VERSION to "DEFAULT",
)

private val EMPTY_SET = setOf<String>()

/**
 * 认证协议常量
 */
internal val AUTH_PROTOCOl_PAP = "PAP"
internal val AUTH_PROTOCOL_MSCHAPv2 = "MSCHAPv2"
internal val AUTH_PROTOCOL_EAP_MSCHAPv2 = "EAP-MSCHAPv2"

/**
 * DEFAULT_SET_MAP
 * 定义所有集合型配置项的默认值。
 */
internal val DEFAULT_SET_MAP = mapOf(
    OscPrefKey.SSL_SUITES to EMPTY_SET,
    OscPrefKey.PPP_AUTH_PROTOCOLS to setOf(AUTH_PROTOCOl_PAP, AUTH_PROTOCOL_MSCHAPv2),
    OscPrefKey.ROUTE_ALLOWED_APPS to EMPTY_SET,
)

/**
 * DEFAULT_URI_MAP
 * 定义所有 Uri 类型配置项的默认值。
 */
internal val DEFAULT_URI_MAP = mapOf<OscPrefKey, Uri?>(
    OscPrefKey.SSL_CERT_DIR to null,
    OscPrefKey.LOG_DIR to null,
)

/**
 * 临时和配置文件相关的键名前缀
 */
internal const val TEMP_KEY_HEADER = "_"
internal const val PROFILE_KEY_HEADER = "PROFILE."