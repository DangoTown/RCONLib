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

    init {
        try {
            this.conn = Socket(host, port ?: 25575)
        } catch (_: Exception) {
            throw ConnectFailedException("Connect to $host:$port failed, please check the host and port.")
        }
    }

    private fun parseInt(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toInt() shl (8 * i))
        }
        return result
    }

    private fun decodeMessage(msg: ByteArray): Message {
        val length = parseInt(msg.sliceArray(0..3))
        val id = parseInt(msg.sliceArray(4..7))
        val type = MessageType.fromInt(parseInt(msg.sliceArray(8..11)))
        val body = msg.sliceArray(12 until (4 + length - 2)).decodeToString()  // exclude 2 null bytes
        return Message(id, type, body)
    }

    private fun encodeMessage(msg: Message): ByteArray {
        val bodyBytes = msg.body.toByteArray()
        val buf = ByteBuffer.allocate(4 + msg.length)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(msg.length)
        buf.putInt(msg.id)
        buf.putInt(msg.type.value)
        buf.put(bodyBytes)
        buf.put(byteArrayOf(0, 0))
        buf.flip()

        val bytes = ByteArray(buf.limit())
        buf.get(bytes)
        return bytes
    }

    private fun sendMessage(messageType: MessageType, body: String): Message {
        val id = lastId.incrementAndGet()
        val message = Message(id, messageType, body)

        val bytes = encodeMessage(message)
        conn.outputStream.write(bytes)
        conn.outputStream.flush()

        val lengthBytes = conn.inputStream.readNBytes(4)
        val length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
        val bodyBytes = conn.inputStream.readNBytes(length)

        val fullBytes = ByteArray(4 + length)
        System.arraycopy(lengthBytes, 0, fullBytes, 0, 4)
        System.arraycopy(bodyBytes, 0, fullBytes, 4, length)

        return decodeMessage(fullBytes)
    }

    fun authenticate(password: String) {
        val response = sendMessage(MessageType.AUTHENTICATE, password)
        if (response.id == -1) {
            throw AuthFailedException("Password is not correct!")
        }
    }

    fun executeCommand(body: String): Message {
        return sendMessage(MessageType.COMMAND, body)
    }

    fun close() {
        conn.close()
    }
}
