package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.Note
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileManagerHelper {

  /**
   * Reads raw text content from any Android SAF Uri.
   */
  fun readTextFromUri(context: Context, uri: Uri): String {
    val stringBuilder = StringBuilder()
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
      BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
        var line: String? = reader.readLine()
        while (line != null) {
          stringBuilder.append(line).append("\n")
          line = reader.readLine()
        }
      }
    }
    return stringBuilder.toString()
  }

  /**
   * Saves text or custom binary content directly back to an existing SAF Uri.
   */
  fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
    return try {
      context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
        outputStream.write(content.toByteArray(Charsets.UTF_8))
        outputStream.flush()
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Writes content according to specified extension. If .pdf, generates real PDF document.
   */
  fun writeNoteToStream(note: Note, outputStream: OutputStream) {
    val cleanExt = note.fileExtension.trim().lowercase().removePrefix(".")
    if (cleanExt == "pdf") {
      writeNoteAsPdf(note, outputStream)
    } else {
      outputStream.write(note.content.toByteArray(Charsets.UTF_8))
      outputStream.flush()
    }
  }

  /**
   * Generates a clean, readable PDF document using Android's native PdfDocument.
   */
  fun writeNoteAsPdf(note: Note, outputStream: OutputStream) {
    val document = PdfDocument()
    val pageWidth = 595 // A4 standard width in points (72 dpi)
    val pageHeight = 842 // A4 standard height
    val margin = 40f
    val contentWidth = pageWidth - (margin * 2)

    val titlePaint = Paint().apply {
      color = Color.BLACK
      textSize = 18f
      isFakeBoldText = true
      isAntiAlias = true
    }

    val datePaint = Paint().apply {
      color = Color.DKGRAY
      textSize = 10f
      isAntiAlias = true
    }

    val bodyPaint = Paint().apply {
      color = Color.rgb(30, 30, 30)
      textSize = 12f
      isAntiAlias = true
    }

    val lineSpacing = 16f
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    var currentY = margin + 20f

    // Draw Title
    val displayTitle = note.title.ifBlank { "Untitled Note" }
    canvas.drawText(displayTitle, margin, currentY, titlePaint)
    currentY += 24f

    // Draw Date & Custom Extension
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
    val metaStr = "வடிவம்: .${note.fileExtension} | புதுப்பிக்கப்பட்டது: $dateStr"
    canvas.drawText(metaStr, margin, currentY, datePaint)
    currentY += 15f

    // Draw separator line
    val linePaint = Paint().apply {
      color = Color.LTGRAY
      strokeWidth = 1f
    }
    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
    currentY += 25f

    // Word wrap and write note content
    val rawLines = note.content.split("\n")
    for (rawLine in rawLines) {
      if (rawLine.isBlank()) {
        currentY += lineSpacing
        if (currentY > pageHeight - margin) {
          document.finishPage(page)
          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
          page = document.startPage(pageInfo)
          canvas = page.canvas
          currentY = margin + 20f
        }
        continue
      }

      val words = rawLine.split(" ")
      var currentLineText = StringBuilder()

      for (word in words) {
        val testLine = if (currentLineText.isEmpty()) word else "$currentLineText $word"
        val textWidth = bodyPaint.measureText(testLine)

        if (textWidth <= contentWidth) {
          currentLineText = StringBuilder(testLine)
        } else {
          canvas.drawText(currentLineText.toString(), margin, currentY, bodyPaint)
          currentY += lineSpacing

          if (currentY > pageHeight - margin) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 20f
          }
          currentLineText = StringBuilder(word)
        }
      }

      if (currentLineText.isNotEmpty()) {
        canvas.drawText(currentLineText.toString(), margin, currentY, bodyPaint)
        currentY += lineSpacing
        if (currentY > pageHeight - margin) {
          document.finishPage(page)
          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
          page = document.startPage(pageInfo)
          canvas = page.canvas
          currentY = margin + 20f
        }
      }
    }

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
  }

  /**
   * Creates a ZIP archive containing all notes with their specified extensions.
   */
  fun createNotesZip(notes: List<Note>): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    ZipOutputStream(byteArrayOutputStream).use { zipOut ->
      val usedFileNames = mutableSetOf<String>()

      // 1. Write each note
      for (note in notes) {
        var baseName = note.getEffectiveFileName()
        var uniqueName = baseName
        var counter = 1
        while (usedFileNames.contains(uniqueName)) {
          val dotIdx = baseName.lastIndexOf('.')
          uniqueName = if (dotIdx > 0) {
            "${baseName.substring(0, dotIdx)}_$counter.${baseName.substring(dotIdx + 1)}"
          } else {
            "${baseName}_$counter"
          }
          counter++
        }
        usedFileNames.add(uniqueName)

        val zipEntry = ZipEntry(uniqueName)
        zipEntry.time = note.updatedAt
        zipOut.putNextEntry(zipEntry)

        val cleanExt = note.fileExtension.trim().lowercase().removePrefix(".")
        if (cleanExt == "pdf") {
          val pdfBytes = ByteArrayOutputStream()
          writeNoteAsPdf(note, pdfBytes)
          zipOut.write(pdfBytes.toByteArray())
        } else {
          zipOut.write(note.content.toByteArray(Charsets.UTF_8))
        }
        zipOut.closeEntry()
      }

      // 2. Add manifest summary in Tamil & English
      val manifestEntry = ZipEntry("backup_manifest.txt")
      zipOut.putNextEntry(manifestEntry)
      val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
      val manifestContent = buildString {
        appendLine("=== பாதுகாப்பான நோட்பேட் பேக்கப் (Secure Notepad Backup) ===")
        appendLine("தேதி / Date: $dateFormatted")
        appendLine("மொத்த குறிப்புகள் / Total Notes: ${notes.size}")
        appendLine("--------------------------------------------------")
        notes.forEachIndexed { index, n ->
          appendLine("${index + 1}. ${n.getEffectiveFileName()} (நீட்சி / Ext: .${n.fileExtension}, பூட்டப்பட்டது: ${if (n.isBiometricLocked) "ஆம்" else "இல்லை"})")
        }
      }
      zipOut.write(manifestContent.toByteArray(Charsets.UTF_8))
      zipOut.closeEntry()

      zipOut.finish()
    }
    return byteArrayOutputStream.toByteArray()
  }

  /**
   * Queries display name from SAF Uri.
   */
  fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
      val cursor = context.contentResolver.query(uri, null, null, null, null)
      cursor?.use {
        if (it.moveToFirst()) {
          val colIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (colIndex != -1) {
            result = it.getString(colIndex)
          }
        }
      }
    }
    if (result == null) {
      result = uri.path
      val cut = result?.lastIndexOf('/')
      if (cut != null && cut != -1) {
        result = result?.substring(cut + 1)
      }
    }
    return result ?: "opened_file.txt"
  }
}
