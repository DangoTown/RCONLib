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

import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger


class RCon(host: String, port: Int) {
    private val conn = Socket(host, port)
    private val lastId = AtomicInteger(0)
    private val headerSize: Int = 10

    private fun parseInt(b: ByteArray): Int {
        var result = 0
        for (i in b.indices) {
            result = result or (b[i].toInt() shl 8 * i)
        }
        return result
    }

    private fun decodeMessage(msg: ByteArray): Message {
        val resp = Message()
        resp.length = this.parseInt(msg.sliceArray(IntRange(0, 3)))
        resp.id = this.parseInt(msg.sliceArray(IntRange(4, 7)))
        resp.type = MessageType.fromInt(this.parseInt(msg.sliceArray(IntRange(8, 11))))
        resp.body = String(msg.sliceArray(IntRange(12, resp.length + 1)))

        return resp
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


    fun authenticate(password: String) {
        sendMessage(MessageType.AUTHENTICATE, password)
    }

    fun sendCommand(body: String): Message {
        return sendMessage(MessageType.COMMAND, body)
    }

    fun sendMessage(messageType: MessageType, body: String): Message {
        val message = Message()
        message.length = body.length + headerSize
        message.id = lastId.addAndGet(1)
        message.type = messageType
        message.body = body

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
}
