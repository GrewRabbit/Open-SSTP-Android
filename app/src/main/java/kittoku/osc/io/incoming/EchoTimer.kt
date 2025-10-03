/**
 * 文件：app/src/main/java/kittoku/osc/io/incoming/EchoTimer.kt
 * 作用说明：
 * 本文件定义了 EchoTimer 类，用于管理 VPN 会话中的 Echo（心跳）机制，定时检测连接是否存活，超时则触发心跳包发送或判定连接断开。
 * 主要功能包括：定时判断是否需要发送 Echo、等待 Echo 响应、超时判定。
 * 在本软件项目中，VPN 数据通道管理模块会调用该文件，用于实现 SSTP 协议的 Echo 检查，保障连接稳定性。
 * 本文件会调用标准库 System.currentTimeMillis，用于时间判断；通过传入的 echoFunction 回调实现心跳包发送，不主动调用其他业务模块。
 */

package kittoku.osc.io.incoming

/**
 * Echo 定时器类，负责定时检测连接状态并触发心跳包发送
 * @param interval 心跳检测间隔（毫秒）
 * @param echoFunction 发送 Echo 的挂起函数
 */
internal class EchoTimer(private val interval: Long, private val echoFunction: suspend () -> Unit) {
    // 上次 tick 的时间戳
    private var lastTicked = 0L
    // Echo 响应超时时间戳
    private var deadline = 0L

    // 是否正在等待 Echo 响应
    private var isEchoWaited = false

    // 判断是否超过心跳检测间隔
    private val isOutOfTime: Boolean
        get() = System.currentTimeMillis() - lastTicked > interval

    // 判断是否超过 Echo 响应超时时间
    private val isDead: Boolean
        get() = System.currentTimeMillis() > deadline

    /**
     * 检查连接是否存活，必要时发送 Echo
     * @return true 表示连接存活，false 表示超时断开
     */
    internal suspend fun checkAlive(): Boolean {
        if (isOutOfTime) {
            if (isEchoWaited) {
                // 已发送 Echo，等待响应，若超时则判定断开
                if (isDead) {
                    return false
                }
            } else {
                // 未发送 Echo，触发心跳包发送并设置超时时间
                echoFunction.invoke()
                isEchoWaited = true
                deadline = System.currentTimeMillis() + interval
            }
        }
        return true
    }

    /**
     * 连接有数据活动时调用，重置心跳检测状态
     */
    internal fun tick() {
        lastTicked = System.currentTimeMillis()
        isEchoWaited = false
    }
}