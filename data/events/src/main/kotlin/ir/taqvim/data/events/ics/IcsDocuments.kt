/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Why a document could not be read or written. */
enum class DocumentError {
    NOT_FOUND,
    ACCESS_DENIED,
    TOO_LARGE,
    IO_ERROR,
}

/** Outcome of reading a document. */
sealed interface DocumentRead {
    data class Text(
        val text: String,
    ) : DocumentRead

    data class Failed(
        val error: DocumentError,
    ) : DocumentRead
}

/** Outcome of writing a document. */
sealed interface DocumentWrite {
    data object Written : DocumentWrite

    data class Failed(
        val error: DocumentError,
    ) : DocumentWrite
}

/** UTF-8 text of at most a given size from a stream. */
internal object BoundedText {
    private const val BUFFER_BYTES = 8_192
    private const val BYTE_ORDER_MARK_CODE = 0xFEFF
    private val BYTE_ORDER_MARK = Char(BYTE_ORDER_MARK_CODE).toString()

    /** The UTF-8 text of [stream] without a leading byte-order mark, or `null` when it is longer than [maxBytes]. */
    fun readUtf8(
        stream: InputStream,
        maxBytes: Int,
    ): String? {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        var count = stream.read(buffer)
        while (count >= 0 && bytes.size() <= maxBytes) {
            bytes.write(buffer, 0, count)
            count = stream.read(buffer)
        }
        return bytes
            .takeIf { it.size() <= maxBytes }
            ?.toByteArray()
            ?.decodeToString()
            ?.removePrefix(BYTE_ORDER_MARK)
    }
}

/** Reads and writes `.ics` documents chosen through the Storage Access Framework (T-1003). */
class IcsDocuments(
    private val resolver: ContentResolver,
    private val maxBytes: Int = DEFAULT_MAX_BYTES,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun read(uri: Uri): DocumentRead =
        withContext(dispatcher) {
            runCatching {
                val stream = resolver.openInputStream(uri)
                val text = stream?.use { BoundedText.readUtf8(it, maxBytes) }
                when {
                    stream == null -> DocumentRead.Failed(DocumentError.IO_ERROR)
                    text == null -> DocumentRead.Failed(DocumentError.TOO_LARGE)
                    else -> DocumentRead.Text(text)
                }
            }.getOrElse { DocumentRead.Failed(errorOf(it)) }
        }

    /** Replaces the content of [uri] with [text] in UTF-8. */
    suspend fun write(
        uri: Uri,
        text: String,
    ): DocumentWrite =
        withContext(dispatcher) {
            runCatching {
                resolver
                    .openOutputStream(uri, TRUNCATE_MODE)
                    ?.use { it.write(text.encodeToByteArray()) }
                    ?.let { DocumentWrite.Written }
                    ?: DocumentWrite.Failed(DocumentError.IO_ERROR)
            }.getOrElse { DocumentWrite.Failed(errorOf(it)) }
        }

    private fun errorOf(failure: Throwable): DocumentError =
        when (failure) {
            is FileNotFoundException -> DocumentError.NOT_FOUND
            is SecurityException -> DocumentError.ACCESS_DENIED
            else -> DocumentError.IO_ERROR
        }

    companion object {
        /** Largest document read by default: 5 MiB. */
        const val DEFAULT_MAX_BYTES: Int = 5 * 1_024 * 1_024
        private const val TRUNCATE_MODE = "wt"
    }
}
