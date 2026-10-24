package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Note
import com.example.util.FileManagerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Secure Notepad", appName)
  }

  @Test
  fun `custom extension preserved verbatim without forced auto extension`() {
    val note1 = Note(
      title = "Secret Plan",
      content = "Confidential text",
      fileExtension = "pdf",
      customFileName = "my_classified_plan.pdf"
    )
    assertEquals("my_classified_plan.pdf", note1.getEffectiveFileName())

    val note2 = Note(
      title = "Shopping List",
      content = "Milk, Bread",
      fileExtension = "md",
      customFileName = ""
    )
    assertEquals("Shopping_List.md", note2.getEffectiveFileName())
  }

  @Test
  fun `zip archive packaging creates valid zip stream`() {
    val notes = listOf(
      Note(title = "Note 1", content = "Hello World", fileExtension = "txt"),
      Note(title = "Note 2", content = "Data in json", fileExtension = "json")
    )
    val zipBytes = FileManagerHelper.createNotesZip(notes)
    assertNotNull(zipBytes)
    assertTrue(zipBytes.isNotEmpty())

    // Verify zip entries
    val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
    val entries = mutableListOf<String>()
    var entry = zis.nextEntry
    while (entry != null) {
      entries.add(entry.name)
      entry = zis.nextEntry
    }
    assertTrue(entries.any { it.contains("Note_1.txt") })
    assertTrue(entries.any { it.contains("Note_2.json") })
    assertTrue(entries.contains("backup_manifest.txt"))
  }
}
