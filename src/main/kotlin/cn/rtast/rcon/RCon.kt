/*
 * Copyright 2023 RTAkland
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cn.rtast.rcon

import cn.rtast.rcon.enums.Message
import cn.rtast.rcon.enums.MessageType
import cn.rtast.rcon.exceptions.AuthFailedException
import cn.rtast.rcon.exceptions.ConnectFailedException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger


class RCon(host: String, port: Int?) {
    private var conn: Socket
    private val lastId = AtomicInteger(0)
    private val headerSize: Int = 10

    init {
        try {
            if (port == null) {
                this.conn = Socket(host, 25575)
            } else {
                this.conn = Socket(host, port)
            }
        } catch (_: Exception) {
            throw ConnectFailedException("Connect to $host:$port failed, please checkout the host and port.")
        }
    }

    private fun parseInt(b: ByteArray): Int {
        var result = 0
        for (i in b.indices) {
            result = result or (b[i].toInt() shl 8 * i)
        }
        return result
    }

    private fun decodeMessage(msg: ByteArray): Message {
        val length = this.parseInt(msg.sliceArray(IntRange(0, 3)))
        val id = this.parseInt(msg.sliceArray(IntRange(4, 7)))
        val type = MessageType.fromInt(this.parseInt(msg.sliceArray(IntRange(8, 11))))
        val body = msg.sliceArray(IntRange(12, length + 1)).decodeToString()
        return Message(length, id, type, body)
    }

    private fun encodeMessage(msg: Message): ByteArray {
        val buf: ByteBuffer = ByteBuffer.allocate(msg.length + 4)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(msg.length)
        buf.putInt(msg.id)
        buf.putInt(msg.type.value)
        buf.put(msg.body.toByteArray())
        buf.put(byteArrayOf(0, 0))
        buf.flip()

        val bytes = ByteArray(msg.length + 4)
        buf.get(bytes, 0, bytes.size)
        return bytes
    }

    private fun sendMessage(messageType: MessageType, body: String): Message {
        val length = body.length + headerSize
        val id = lastId.addAndGet(1)
        val message = Message(length, id, messageType, body)

        val bytes: ByteArray = this.encodeMessage(message)
        conn.outputStream.write(bytes)
        conn.outputStream.flush()

        val lengthBytes: ByteArray = conn.inputStream.readNBytes(4)
        val lengthBuf: ByteBuffer = ByteBuffer.wrap(lengthBytes)
        lengthBuf.order(ByteOrder.LITTLE_ENDIAN)

        val respSize: Int = lengthBuf.getInt()
        lengthBuf.flip()

        val respBuf: ByteBuffer = ByteBuffer.allocate(respSize + 4).put(lengthBuf)
        respBuf.order(ByteOrder.LITTLE_ENDIAN)
        respBuf.put(conn.inputStream.readNBytes(respSize))
        respBuf.flip()

        val respBytes = ByteArray(respSize + 4)
        respBuf.get(respBytes, 0, respBytes.size)
        return this.decodeMessage(respBytes)
    }

    fun authenticate(password: String) {
        val response = this.sendMessage(MessageType.AUTHENTICATE, password)
        if (response.id == -1) {
            throw AuthFailedException("Password is not correct!")
        }
    }

    fun executeCommand(body: String): Message {
        return this.sendMessage(MessageType.COMMAND, body)
    }

    fun close() {
        conn.close()
    }
}
