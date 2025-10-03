/**
 * 文件：app/src/main/java/kittoku/osc/configuration.kt
 * 作用说明：
 * 本文件定义了 PPP/SSTP 网络协议相关的常用配置参数，包括 MTU（最大传输单元）、MRU（最大接收单元）的默认值、最大值和最小值。
 * 主要功能：
 * - 提供 PPP/SSTP 协议协商和链路建立过程中所需的参数常量，保证数据包大小在合理范围内。
 * 在本项目中，PPP/SSTP 协议实现模块（如链路协商、帧处理、连接管理等）会调用本文件，
 * 用于获取和校验 MTU/MRU 参数，确保数据传输的兼容性和稳定性。
 * 本文件只定义常量，不会主动调用其它文件，但会被协议相关的实现文件频繁依赖和调用。
 */

package kittoku.osc

// PPP/SSTP 协议相关的 MTU/MRU 参数常量定义
internal const val MAX_MRU = 2000      // 最大接收单元
internal const val MAX_MTU = 2000      // 最大传输单元
internal const val MIN_MRU = 68        // 最小接收单元
internal const val MIN_MTU = 68        // 最小传输单元
internal const val DEFAULT_MRU = 1500  // 默认接收单元
internal const val DEFAULT_MTU = 1500  // 默认传输单元