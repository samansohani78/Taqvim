/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/** T-1003 (R): reading and writing `.ics` documents through the content resolver (SAF URIs behave the same way). */
@RunWith(AndroidJUnit4::class)
class IcsDocumentsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val documents = IcsDocuments(context.contentResolver)

    private fun file(name: String) = File(context.cacheDir, name)

    @Test
    fun writesAndReadsUtf8Documents(): Unit =
        runTest {
            val uri = Uri.fromFile(file("calendar.ics"))
            val text = "BEGIN:VCALENDAR\r\nSUMMARY:نوروز\r\nEND:VCALENDAR\r\n"

            documents.write(uri, text) shouldBe DocumentWrite.Written
            documents.read(uri) shouldBe DocumentRead.Text(text)
            documents.write(uri, "short") shouldBe DocumentWrite.Written
            documents.read(uri) shouldBe DocumentRead.Text("short")

            val byteOrderMark = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            file("bom.ics").writeBytes(byteOrderMark + "X".encodeToByteArray())
            documents.read(Uri.fromFile(file("bom.ics"))) shouldBe DocumentRead.Text("X")
        }

    @Test
    fun missingOversizedAndUnwritableDocumentsAreTyped(): Unit =
        runTest {
            documents.read(Uri.fromFile(file("missing.ics"))) shouldBe DocumentRead.Failed(DocumentError.NOT_FOUND)

            val ten = file("ten.ics").apply { writeText("0123456789") }
            IcsDocuments(context.contentResolver, maxBytes = 9).read(Uri.fromFile(ten)) shouldBe
                DocumentRead.Failed(DocumentError.TOO_LARGE)
            IcsDocuments(context.contentResolver, maxBytes = 10).read(Uri.fromFile(ten)) shouldBe
                DocumentRead.Text("0123456789")

            documents.write(Uri.fromFile(context.cacheDir), "x").shouldBeInstanceOf<DocumentWrite.Failed>()
            val unknownProvider = Uri.parse("content://ir.taqvim.nobody/calendar.ics")
            documents.read(unknownProvider).shouldBeInstanceOf<DocumentRead.Failed>()
        }
}
