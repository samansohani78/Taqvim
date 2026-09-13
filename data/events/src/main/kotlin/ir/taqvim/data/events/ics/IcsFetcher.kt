/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A subscription URL after validation. */
sealed interface SubscriptionUrl {
    /** The feed at [url], always `https:`. */
    data class Valid(
        val url: String,
    ) : SubscriptionUrl

    /** An `http:` feed: subscriptions are fetched over HTTPS only. */
    data object Insecure : SubscriptionUrl

    /** Not an absolute `https:`, `webcal:` or `webcals:` URL with a host. */
    data object Invalid : SubscriptionUrl
}

/** Validation of subscription URLs; `webcal:` and `webcals:` are read as `https:`. */
object SubscriptionUrls {
    private val SECURE_SCHEMES = setOf("https", "webcal", "webcals")

    fun normalize(text: String): SubscriptionUrl {
        val trimmed = text.trim()
        val uri = runCatching { URI(trimmed) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        return when {
            uri == null || scheme == null || uri.host.isNullOrEmpty() -> SubscriptionUrl.Invalid
            scheme == "http" -> SubscriptionUrl.Insecure
            scheme in SECURE_SCHEMES -> SubscriptionUrl.Valid("https" + trimmed.substring(scheme.length))
            else -> SubscriptionUrl.Invalid
        }
    }
}

/** The validators of a previous response, sent as `If-None-Match` and `If-Modified-Since` (RFC 9110 §13.1). */
data class HttpValidators(
    val etag: String? = null,
    val lastModified: String? = null,
)

/** Why a feed could not be fetched. */
sealed interface FetchError {
    data class HttpStatus(
        val code: Int,
    ) : FetchError

    data object TooLarge : FetchError

    data object Timeout : FetchError

    data object Network : FetchError

    /** A redirect left the original scheme (e.g. HTTPS to HTTP). */
    data object InsecureRedirect : FetchError

    data object InvalidUrl : FetchError
}

/** Outcome of fetching a feed. */
sealed interface FetchResult {
    data class Modified(
        val body: String,
        val validators: HttpValidators,
    ) : FetchResult

    /** `304 Not Modified`: the cached copy is current. */
    data object NotModified : FetchResult

    data class Failed(
        val error: FetchError,
    ) : FetchResult
}

/** Fetches iCalendar feeds. */
interface IcsFetcher {
    suspend fun fetch(
        url: String,
        validators: HttpValidators,
    ): FetchResult
}

/** Limits of one fetch. */
data class FetchLimits(
    val maxBytes: Int = IcsDocuments.DEFAULT_MAX_BYTES,
    val connectTimeout: Duration = DEFAULT_CONNECT_TIMEOUT,
    val readTimeout: Duration = DEFAULT_READ_TIMEOUT,
) {
    private companion object {
        val DEFAULT_CONNECT_TIMEOUT = 15.seconds
        val DEFAULT_READ_TIMEOUT = 30.seconds
    }
}

/** [IcsFetcher] over the platform `HttpURLConnection`, with conditional requests and a size limit. */
class HttpIcsFetcher(
    private val limits: FetchLimits = FetchLimits(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IcsFetcher {
    override suspend fun fetch(
        url: String,
        validators: HttpValidators,
    ): FetchResult =
        withContext(dispatcher) {
            val target = runCatching { URI(url).toURL() }.getOrNull()
            val connection = target?.let { runCatching { it.openConnection() }.getOrNull() as? HttpURLConnection }
            if (target == null || connection == null) {
                FetchResult.Failed(FetchError.InvalidUrl)
            } else {
                runCatching { exchange(connection, target, validators) }
                    .also { connection.disconnect() }
                    .getOrElse {
                        FetchResult.Failed(
                            if (it is SocketTimeoutException) FetchError.Timeout else FetchError.Network,
                        )
                    }
            }
        }

    private fun exchange(
        connection: HttpURLConnection,
        target: URL,
        validators: HttpValidators,
    ): FetchResult {
        connection.connectTimeout = limits.connectTimeout.inWholeMilliseconds.toInt()
        connection.readTimeout = limits.readTimeout.inWholeMilliseconds.toInt()
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "text/calendar, */*;q=0.1")
        validators.etag?.let { connection.setRequestProperty("If-None-Match", it) }
        validators.lastModified?.let { connection.setRequestProperty("If-Modified-Since", it) }
        val status = connection.responseCode
        return when {
            connection.url.protocol != target.protocol -> FetchResult.Failed(FetchError.InsecureRedirect)
            status == HttpURLConnection.HTTP_NOT_MODIFIED -> FetchResult.NotModified
            status in SUCCESS -> body(connection)
            else -> FetchResult.Failed(FetchError.HttpStatus(status))
        }
    }

    private fun body(connection: HttpURLConnection): FetchResult {
        val text =
            connection
                .takeIf { it.contentLengthLong <= limits.maxBytes }
                ?.inputStream
                ?.use { BoundedText.readUtf8(it, limits.maxBytes) }
                ?: return FetchResult.Failed(FetchError.TooLarge)
        return FetchResult.Modified(
            text,
            HttpValidators(connection.getHeaderField("ETag"), connection.getHeaderField("Last-Modified")),
        )
    }

    private companion object {
        val SUCCESS = HttpURLConnection.HTTP_OK..<HttpURLConnection.HTTP_MULT_CHOICE
    }
}
