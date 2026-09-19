/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.jupiter.api.Test

/** T-1800: what the 16 KB page-size check accepts and what it reports. */
class PageAlignmentTest {
    private fun library(
        path: String = "lib/arm64-v8a/libfoo.so",
        stored: Boolean = true,
        dataOffset: Long = 3 * PageAlignment.PAGE_BYTES,
        alignments: List<Long> = listOf(PageAlignment.PAGE_BYTES, PageAlignment.PAGE_BYTES),
    ) = NativeLibrary(path, stored, dataOffset, alignments)

    @Test
    fun `a stored, aligned library with 16 KB segments passes`() {
        PageAlignment.problems(listOf(library())) shouldBe emptyList()
    }

    @Test
    fun `a larger alignment also passes, since a 64 KB page device maps it too`() {
        PageAlignment.problems(listOf(library(alignments = listOf(65_536)))) shouldBe emptyList()
    }

    @Test
    fun `a compressed library is reported`() {
        val problems = PageAlignment.problems(listOf(library(stored = false)))

        problems.size shouldBe 1
        problems.single() shouldContain "compressed"
    }

    @Test
    fun `a library that does not start on a page boundary is reported`() {
        val problems = PageAlignment.problems(listOf(library(dataOffset = 4_096)))

        problems.single() shouldContain "not a multiple of 16384"
    }

    @Test
    fun `4 KB segments are reported, which is what breaks a 16 KB page device`() {
        val problems = PageAlignment.problems(listOf(library(alignments = listOf(16_384, 4_096))))

        problems.single() shouldContain "4096"
    }

    @Test
    fun `a file without LOAD segments is reported`() {
        val problems = PageAlignment.problems(listOf(library(alignments = emptyList())))

        problems.single() shouldContain "not an ELF file"
    }

    @Test
    fun `reads the LOAD alignments of a 64-bit ELF file`() {
        val elf = elf64(loadAlignments = listOf(16_384L, 16_384L))

        ElfAlignments.read(elf) shouldBe listOf(16_384L, 16_384L)
    }

    @Test
    fun `ignores program headers that are not LOAD`() {
        val elf = elf64(loadAlignments = listOf(16_384L), extraNonLoad = true)

        ElfAlignments.read(elf) shouldBe listOf(16_384L)
    }

    @Test
    fun `a file that is not an ELF file has no alignments`() {
        ElfAlignments.read(ByteArray(128)) shouldBe emptyList()
        ElfAlignments.read("no".toByteArray()) shouldBe emptyList()
    }

    @Test
    fun `reads a stored library out of an APK, with its data offset and alignments`() {
        val elf = elf64(loadAlignments = listOf(16_384L))
        val apk = zip(mapOf("lib/arm64-v8a/libfoo.so" to elf, "classes.dex" to ByteArray(8)))

        val libraries = ApkNativeLibraries.read(apk)

        libraries.size shouldBe 1
        val library = libraries.single()
        library.path shouldBe "lib/arm64-v8a/libfoo.so"
        library.stored shouldBe true
        library.loadAlignments shouldBe listOf(16_384L)
        PageAlignment.problems(libraries).size shouldBe 1 // the test zip does not pad to a page boundary
    }

    @Test
    fun `an APK without native libraries has nothing to check`() {
        ApkNativeLibraries.read(zip(mapOf("classes.dex" to ByteArray(4)))) shouldBe emptyList()
        PageAlignment.problems(emptyList()) shouldBe emptyList()
    }

    /** A zip with stored entries, like the `lib/` entries of an APK. */
    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val bytes = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bytes).use { out ->
            out.setMethod(java.util.zip.ZipOutputStream.STORED)
            entries.forEach { (name, content) ->
                val entry = java.util.zip.ZipEntry(name)
                entry.size = content.size.toLong()
                entry.compressedSize = content.size.toLong()
                entry.crc =
                    java.util.zip
                        .CRC32()
                        .apply { update(content) }
                        .value
                out.putNextEntry(entry)
                out.write(content)
                out.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    /** A minimal 64-bit little-endian ELF file with one program header per alignment. */
    private fun elf64(
        loadAlignments: List<Long>,
        extraNonLoad: Boolean = false,
    ): ByteArray {
        val headers = loadAlignments.size + if (extraNonLoad) 1 else 0
        val entrySize = 56
        val start = 64
        val bytes = ByteArray(start + headers * entrySize)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0, 0x464C457F)
        bytes[4] = 2 // 64-bit
        bytes[5] = 1 // little-endian
        buffer.putLong(32, start.toLong())
        buffer.putShort(54, entrySize.toShort())
        buffer.putShort(56, headers.toShort())
        loadAlignments.forEachIndexed { index, alignment ->
            val at = start + index * entrySize
            buffer.putInt(at, 1) // PT_LOAD
            buffer.putLong(at + 0x30, alignment)
        }
        if (extraNonLoad) {
            val at = start + loadAlignments.size * entrySize
            buffer.putInt(at, 6) // PT_PHDR
            buffer.putLong(at + 0x30, 8)
        }
        return bytes
    }
}
