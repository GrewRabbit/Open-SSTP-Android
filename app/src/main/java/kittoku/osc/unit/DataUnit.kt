/**
 * 文件：app/src/main/java/kittoku/osc/unit/DataUnit.kt
 * 作用说明：
 * 本文件定义了数据单元抽象基类 DataUnit，用于统一管理协议数据结构的序列化与反序列化。
 * 主要功能：
 * - DataUnit：抽象基类，规定 length、write、read 等接口，便于各协议帧和属性实现具体的数据读写逻辑。
 * 在本项目中，所有协议相关的数据结构（如 PPP、SSTP 的帧和属性类）都会继承并实现本类，
 * 例如 Frame、Attribute、ControlPacket 等类会调用本文件，实现具体协议的数据处理。
 * 本文件不会主动调用其它文件，但会被大量协议实现文件依赖和调用。
 */

package kittoku.osc.unit

import java.nio.ByteBuffer

/**
 * 数据单元抽象基类
 * 统一 length、write、read 接口
 */
internal abstract class DataUnit {
    // 当前数据单元的字节长度
    internal abstract val length: Int

    // 将数据单元内容写入 ByteBuffer
    internal abstract fun write(buffer: ByteBuffer)

    // 从 ByteBuffer 读取数据单元内容
    internal abstract fun read(buffer: ByteBuffer)

    /**
     * 将数据单元序列化为 ByteBuffer
     */
    internal fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(length)
        write(buffer)
        buffer.flip()
        return buffer
    }
}