/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1003 (U): conditional fetches against a local HTTP server (JDK `com.sun.net.httpserver`). */
class HttpIcsFetcherTest {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    private val requests = ConcurrentLinkedQueue<Map<String, String?>>()
    private val body = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"

    private val base: String
        get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/cal.ics") { exchange ->
            val headers = exchange.requestHeaders
            requests += listOf("Accept", "If-None-Match", "If-Modified-Since").associateWith { headers.getFirst(it) }
            if (headers.getFirst("If-None-Match") == "\"v1\"") {
                exchange.sendResponseHeaders(304, -1)
                exchange.close()
            } else {
                exchange.responseHeaders.add("ETag", "\"v1\"")
                exchange.responseHeaders.add("Last-Modified", "Sun, 13 Sep 2026 10:00:00 GMT")
                exchange.send(200, body, chunked = false)
            }
        }
        server.createContext("/chunked.ics") { it.send(200, body, chunked = true) }
        server.createContext("/missing.ics") { it.send(404, "gone", chunked = false) }
        server.createContext("/slow.ics") { exchange ->
            Thread.sleep(SLOW_MILLIS)
            exchange.send(200, body, chunked = false)
        }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
    }

    private fun HttpExchange.send(
        status: Int,
        text: String,
        chunked: Boolean,
    ) {
        val bytes = text.encodeToByteArray()
        sendResponseHeaders(status, if (chunked) 0 else bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    @Test
    fun `a full response returns the body and its validators, a matching ETag returns not modified`(): Unit =
        runTest {
            val fetcher = HttpIcsFetcher()

            fetcher.fetch("$base/cal.ics", HttpValidators()) shouldBe
                FetchResult.Modified(body, HttpValidators("\"v1\"", "Sun, 13 Sep 2026 10:00:00 GMT"))
            fetcher.fetch("$base/cal.ics", HttpValidators("\"v1\"", "Sun, 13 Sep 2026 10:00:00 GMT")) shouldBe
                FetchResult.NotModified
            requests.toList() shouldBe
                listOf(
                    mapOf("Accept" to "text/calendar, */*;q=0.1", "If-None-Match" to null, "If-Modified-Since" to null),
                    mapOf(
                        "Accept" to "text/calendar, */*;q=0.1",
                        "If-None-Match" to "\"v1\"",
                        "If-Modified-Since" to "Sun, 13 Sep 2026 10:00:00 GMT",
                    ),
                )
        }

    @Test
    fun `status, size, timeout and connection failures are typed`(): Unit =
        runTest {
            HttpIcsFetcher().fetch("$base/missing.ics", HttpValidators()) shouldBe
                FetchResult.Failed(FetchError.HttpStatus(404))
            val small = HttpIcsFetcher(FetchLimits(maxBytes = 10))
            small.fetch("$base/cal.ics", HttpValidators()) shouldBe FetchResult.Failed(FetchError.TooLarge)
            small.fetch("$base/chunked.ics", HttpValidators()) shouldBe FetchResult.Failed(FetchError.TooLarge)
            val impatient = HttpIcsFetcher(FetchLimits(readTimeout = 200.milliseconds))
            impatient.fetch("$base/slow.ics", HttpValidators()) shouldBe
                FetchResult.Failed(FetchError.Timeout)
            HttpIcsFetcher().fetch("::not a url", HttpValidators()) shouldBe FetchResult.Failed(FetchError.InvalidUrl)
            HttpIcsFetcher().fetch("mailto:someone@example.org", HttpValidators()) shouldBe
                FetchResult.Failed(FetchError.InvalidUrl)
            val port = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
            HttpIcsFetcher().fetch("http://127.0.0.1:$port/cal.ics", HttpValidators()) shouldBe
                FetchResult.Failed(FetchError.Network)
        }

    private companion object {
        const val SLOW_MILLIS = 1_500L
    }
}
