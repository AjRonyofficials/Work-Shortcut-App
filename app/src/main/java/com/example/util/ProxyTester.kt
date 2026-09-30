package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object ProxyTester {

    data class PingResult(
        val isSuccess: Boolean,
        val latencyMs: Long,
        val resolvedIp: String? = null,
        val errorMessage: String? = null
    )

    suspend fun testProxy(
        host: String,
        port: Int,
        protocol: String = "SOCKS5",
        timeoutMs: Int = 3000,
        pingOptimized: Boolean = true
    ): PingResult = withContext(Dispatchers.IO) {
        val effectiveTimeout = if (pingOptimized) 2000 else timeoutMs
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null

        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = effectiveTimeout

            val address = InetSocketAddress(host, port)
            socket.connect(address, effectiveTimeout)

            val latency = System.currentTimeMillis() - startTime
            val resolved = address.address?.hostAddress ?: host

            // If SOCKS5, verify handshake
            if (protocol.equals("SOCKS5", ignoreCase = true)) {
                val out: OutputStream = socket.getOutputStream()
                val inStream: InputStream = socket.getInputStream()
                // SOCKS5 client greeting: VER(5), NMETHODS(1), NO_AUTH(0)
                out.write(byteArrayOf(0x05, 0x01, 0x00))
                out.flush()

                val response = ByteArray(2)
                val read = inStream.read(response)
                if (read >= 2 && response[0] == 0x05.toByte()) {
                    return@withContext PingResult(isSuccess = true, latencyMs = latency, resolvedIp = resolved)
                }
            } else {
                // HTTP proxy handshake test
                val out = socket.getOutputStream()
                out.write("HEAD / HTTP/1.1\r\nHost: $host\r\n\r\n".toByteArray())
                out.flush()
            }

            PingResult(isSuccess = true, latencyMs = latency, resolvedIp = resolved)
        } catch (e: Exception) {
            PingResult(
                isSuccess = false,
                latencyMs = -1,
                resolvedIp = host,
                errorMessage = e.message ?: "Connection failed"
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }
}
