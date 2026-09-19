/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A native library packaged in an APK, as the page-size check sees it. */
data class NativeLibrary(
    /** Path inside the APK, e.g. `lib/arm64-v8a/libfoo.so`. */
    val path: String,
    /** Whether the entry is stored uncompressed, which it must be to be mapped from the APK. */
    val stored: Boolean,
    /** Offset of the entry's bytes in the APK. */
    val dataOffset: Long,
    /** `p_align` of every `PT_LOAD` segment of the ELF file. */
    val loadAlignments: List<Long>,
)

/**
 * Devices shipped with Android 15 and later may use 16 KB memory pages (the owner's OnePlus 15 does). A native
 * library is only usable there when its `PT_LOAD` segments are aligned to at least that, and when it is stored
 * uncompressed at a 16 KB boundary of the APK so the loader can map it in place. Both hold today (AGP aligns the
 * entries and the AndroidX libraries are built with a 16 KB alignment), and neither is visible in any test that runs
 * on this machine, so the build checks them.
 */
object PageAlignment {
    /** The largest page size Android devices use. */
    const val PAGE_BYTES: Long = 16_384

    /** What is wrong with [libraries]; empty when every one of them can be mapped on a 16 KB page device. */
    fun problems(libraries: List<NativeLibrary>): List<String> =
        libraries.flatMap { library ->
            buildList {
                if (!library.stored) add("${library.path} is compressed; it cannot be mapped from the APK")
                if (library.dataOffset % PAGE_BYTES != 0L) {
                    add("${library.path} starts at ${library.dataOffset}, not a multiple of $PAGE_BYTES")
                }
                val small = library.loadAlignments.filter { it < PAGE_BYTES }
                if (small.isNotEmpty()) {
                    add("${library.path} has LOAD segments aligned to ${small.joinToString()}, below $PAGE_BYTES")
                }
                if (library.loadAlignments.isEmpty()) add("${library.path} has no LOAD segment; it is not an ELF file")
            }
        }
}

/** Reads the `p_align` of every `PT_LOAD` segment of an ELF file. */
object ElfAlignments {
    private const val MAGIC = 0x464C457F // 0x7F 'E' 'L' 'F', little-endian
    private const val CLASS_64 = 2
    private const val DATA_LITTLE_ENDIAN = 1
    private const val PT_LOAD = 1
    private const val HEADER_BYTES = 64

    /** The alignments, or an empty list when [bytes] is not an ELF file this check understands. */
    fun read(bytes: ByteArray): List<Long> {
        if (bytes.size < HEADER_BYTES) return emptyList()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.getInt(0) != MAGIC) return emptyList()
        val is64 = bytes[4].toInt() == CLASS_64
        buffer.order(if (bytes[5].toInt() == DATA_LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        val offset = if (is64) buffer.getLong(32) else buffer.getInt(28).toLong()
        val entrySize = buffer.getShort(if (is64) 54 else 42).toInt()
        val count = buffer.getShort(if (is64) 56 else 44).toInt()
        return (0 until count).mapNotNull { index ->
            val start = offset + index.toLong() * entrySize
            if (start + entrySize > bytes.size) return@mapNotNull null
            val at = start.toInt()
            if (buffer.getInt(at) != PT_LOAD) null else alignment(buffer, at, is64)
        }
    }

    /** `p_align` is the last field of a program header: at 0x30 in a 64-bit one, at 0x1C in a 32-bit one. */
    private fun alignment(
        buffer: ByteBuffer,
        at: Int,
        is64: Boolean,
    ): Long = if (is64) buffer.getLong(at + 0x30) else buffer.getInt(at + 0x1C).toLong()
}

/**
 * The native libraries of an APK, read from the zip itself: the central directory gives each entry's method and the
 * offset of its local header, and the bytes of a stored entry are the ELF file. `java.util.zip` exposes neither the
 * local header offset nor the data offset, which are exactly what the page check needs.
 */
object ApkNativeLibraries {
    private const val CENTRAL_SIGNATURE = 0x02014B50
    private const val EOCD_SIGNATURE = 0x06054B50
    private const val EOCD_BYTES = 22
    private const val LOCAL_HEADER_BYTES = 30
    private const val STORED = 0
    private const val UNSIGNED_SHORT = 0xFFFF

    /** Every native library entry of [apk] (those under `lib/`). */
    fun read(apk: ByteArray): List<NativeLibrary> {
        val buffer = ByteBuffer.wrap(apk).order(ByteOrder.LITTLE_ENDIAN)
        var at = centralDirectoryStart(buffer, apk.size) ?: return emptyList()
        return buildList {
            while (at + 46 <= apk.size && buffer.getInt(at) == CENTRAL_SIGNATURE) {
                val method = buffer.getShort(at + 10).toInt() and UNSIGNED_SHORT
                val compressed = buffer.getInt(at + 20).toLong()
                val nameLength = buffer.getShort(at + 28).toInt() and UNSIGNED_SHORT
                val extraLength = buffer.getShort(at + 30).toInt() and UNSIGNED_SHORT
                val commentLength = buffer.getShort(at + 32).toInt() and UNSIGNED_SHORT
                val headerOffset = buffer.getInt(at + 42).toLong()
                val name = String(apk, at + 46, nameLength, Charsets.UTF_8)
                if (name.startsWith("lib/") && name.endsWith(".so")) {
                    add(library(apk, buffer, name, method, headerOffset, compressed))
                }
                at += 46 + nameLength + extraLength + commentLength
            }
        }
    }

    private fun library(
        apk: ByteArray,
        buffer: ByteBuffer,
        name: String,
        method: Int,
        headerOffset: Long,
        compressed: Long,
    ): NativeLibrary {
        val header = headerOffset.toInt()
        val nameLength = buffer.getShort(header + 26).toInt() and UNSIGNED_SHORT
        val extraLength = buffer.getShort(header + 28).toInt() and UNSIGNED_SHORT
        val data = headerOffset + LOCAL_HEADER_BYTES + nameLength + extraLength
        val stored = method == STORED
        val end = (data + compressed).coerceAtMost(apk.size.toLong()).toInt()
        val bytes = if (stored) apk.copyOfRange(data.toInt(), end) else ByteArray(0)
        return NativeLibrary(name, stored, data, if (stored) ElfAlignments.read(bytes) else listOf(PAGE_PLACEHOLDER))
    }

    /** Scans back for the end-of-central-directory record, which may be followed by a comment. */
    private fun centralDirectoryStart(
        buffer: ByteBuffer,
        size: Int,
    ): Int? {
        for (at in size - EOCD_BYTES downTo 0) {
            if (buffer.getInt(at) == EOCD_SIGNATURE) return buffer.getInt(at + 16)
        }
        return null
    }

    /** A compressed entry is already a problem, so its ELF is never read; this keeps it out of the "no LOAD" report. */
    private const val PAGE_PLACEHOLDER = PageAlignment.PAGE_BYTES
}
