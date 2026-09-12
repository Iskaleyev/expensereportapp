package com.timur.receiptlogger.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.timur.receiptlogger.data.ReceiptEntry
import com.timur.receiptlogger.data.Trip
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal, hand-built .xlsx (OOXML spreadsheet) file, with no external library.
 * Two tabs: "Report" (one row per expense, numbered, with a total) and "Photos" (the same
 * numbering in column 1, with each receipt's photo placed in column 2).
 *
 * Only the small subset of the format this report needs: inline strings, numeric cells,
 * a few cell styles (bold text, currency number format), fixed column widths, and floating
 * images anchored to cells via a drawing part.
 *
 * This exact XML structure was prototyped and round-trip verified (openpyxl + LibreOffice,
 * including a rendered PDF check that the images actually appear) before being ported here.
 */
object XlsxReportWriter {

    /** Photos are downscaled to fit within this many pixels on their longer side. */
    private const val MAX_IMG_DIM_PX = 200
    private const val JPEG_QUALITY = 80

    private data class PhotoItem(
        val number: Int,
        val bytes: ByteArray,
        val widthPx: Int,
        val heightPx: Int
    )

    fun writeTripReport(outputFile: File, trip: Trip, receipts: List<ReceiptEntry>) {
        val dateFmt = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val dateRange = "${dateFmt.format(Date(trip.startDate))} – ${dateFmt.format(Date(trip.endDate))}"
        val total = receipts.sumOf { it.amount }
        val sorted = receipts.sortedBy { it.createTime }

        val photoItems = sorted.mapIndexedNotNull { index, entry ->
            val photo = readAndScalePhoto(entry.photoPath) ?: return@mapIndexedNotNull null
            val (bytes, w, h) = photo
            PhotoItem(number = index + 1, bytes = bytes, widthPx = w, heightPx = h)
        }

        val reportSheetXml = buildReportSheetXml(trip.name, dateRange, sorted, total, dateFmt)
        val photosSheetXml = buildPhotosSheetXml(sorted.size, photoItems)
        val drawingXml = buildDrawingXml(photoItems)
        val drawingRelsXml = buildDrawingRelsXml(photoItems)

        outputFile.parentFile?.mkdirs()
        ZipOutputStream(outputFile.outputStream()).use { zip ->
            writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
            writeEntry(zip, "_rels/.rels", RELS)
            writeEntry(zip, "xl/workbook.xml", WORKBOOK)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            writeEntry(zip, "xl/styles.xml", STYLES)
            writeEntry(zip, "xl/worksheets/sheet1.xml", reportSheetXml)
            writeEntry(zip, "xl/worksheets/sheet2.xml", photosSheetXml)
            writeEntry(zip, "xl/worksheets/_rels/sheet2.xml.rels", SHEET2_RELS)
            writeEntry(zip, "xl/drawings/drawing1.xml", drawingXml)
            writeEntry(zip, "xl/drawings/_rels/drawing1.xml.rels", drawingRelsXml)
            photoItems.forEachIndexed { i, item ->
                writeBinaryEntry(zip, "xl/media/image${i + 1}.jpg", item.bytes)
            }
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeBinaryEntry(zip: ZipOutputStream, name: String, content: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content)
        zip.closeEntry()
    }

    private fun escape(s: String): String = buildString {
        for (ch in s) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i > 0) {
            val rem = (i - 1) % 26
            sb.insert(0, ('A' + rem))
            i = (i - 1) / 26
        }
        return sb.toString()
    }

    private fun cellStr(row: Int, col: Int, text: String, style: Int? = null): String {
        val ref = "${colLetter(col)}$row"
        val sAttr = if (style != null) " s=\"$style\"" else ""
        return "<c r=\"$ref\"$sAttr t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(text)}</t></is></c>"
    }

    private fun cellNum(row: Int, col: Int, value: Double, style: Int? = null): String {
        val ref = "${colLetter(col)}$row"
        val sAttr = if (style != null) " s=\"$style\"" else ""
        return "<c r=\"$ref\"$sAttr><v>$value</v></c>"
    }

    private fun cellInt(row: Int, col: Int, value: Int, style: Int? = null): String {
        val ref = "${colLetter(col)}$row"
        val sAttr = if (style != null) " s=\"$style\"" else ""
        return "<c r=\"$ref\"$sAttr><v>$value</v></c>"
    }

    /** Reads the photo at [path], downscales it to fit [MAX_IMG_DIM_PX], and re-encodes it
     *  as a small JPEG. Returns null if the file is missing or can't be decoded. */
    private fun readAndScalePhoto(path: String): Triple<ByteArray, Int, Int>? {
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, boundsOpts)
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            val sampleSize = calculateInSampleSize(origW, origH, MAX_IMG_DIM_PX)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(path, decodeOpts) ?: return null

            val (targetW, targetH) = scaledSize(decoded.width, decoded.height, MAX_IMG_DIM_PX)
            val scaled = if (targetW != decoded.width || targetH != decoded.height) {
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled.recycle()
            Triple(out.toByteArray(), targetW, targetH)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqDim: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= reqDim && h / 2 >= reqDim) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaledSize(width: Int, height: Int, maxDim: Int): Pair<Int, Int> {
        if (width <= maxDim && height <= maxDim) return width to height
        val scale = maxDim.toDouble() / maxOf(width, height)
        val w = (width * scale).toInt().coerceAtLeast(1)
        val h = (height * scale).toInt().coerceAtLeast(1)
        return w to h
    }

    private fun pxToEmu(px: Int): Long = px.toLong() * 9525L

    private fun buildReportSheetXml(
        tripName: String,
        dateRange: String,
        sorted: List<ReceiptEntry>,
        total: Double,
        dateFmt: DateFormat
    ): String {
        val rows = StringBuilder()
        var r = 1

        rows.append("<row r=\"$r\">${cellStr(r, 1, "Expense Report", style = 3)}</row>"); r++
        rows.append("<row r=\"$r\">${cellStr(r, 1, "Trip:", style = 1)}${cellStr(r, 2, tripName)}</row>"); r++
        rows.append("<row r=\"$r\">${cellStr(r, 1, "Dates:", style = 1)}${cellStr(r, 2, dateRange)}</row>"); r++
        r++ // blank row

        val headerRow = r
        rows.append(
            "<row r=\"$headerRow\">${cellStr(headerRow, 1, "No.", style = 1)}" +
                "${cellStr(headerRow, 2, "Date", style = 1)}" +
                "${cellStr(headerRow, 3, "Amount", style = 1)}${cellStr(headerRow, 4, "Note", style = 1)}</row>"
        )
        r++

        for ((index, entry) in sorted.withIndex()) {
            val dateText = dateFmt.format(Date(entry.timestamp))
            rows.append(
                "<row r=\"$r\">${cellInt(r, 1, index + 1)}${cellStr(r, 2, dateText)}" +
                    "${cellNum(r, 3, entry.amount, style = 2)}${cellStr(r, 4, entry.note)}</row>"
            )
            r++
        }

        r++ // blank row
        rows.append("<row r=\"$r\">${cellStr(r, 2, "Total", style = 1)}${cellNum(r, 3, total, style = 2)}</row>")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols>
<col min="1" max="1" width="6" customWidth="1"/>
<col min="2" max="2" width="14" customWidth="1"/>
<col min="3" max="3" width="12" customWidth="1"/>
<col min="4" max="4" width="40" customWidth="1"/>
</cols>
<sheetData>
$rows
</sheetData>
</worksheet>"""
    }

    /** The "Photos" tab: column 1 is the same expense number as tab 1, column 2 holds
     *  the receipt's photo (placed via a floating drawing anchor, not a cell value). */
    private fun buildPhotosSheetXml(count: Int, photoItems: List<PhotoItem>): String {
        val byNumber = photoItems.associateBy { it.number }
        val rows = StringBuilder()
        rows.append("<row r=\"1\">${cellStr(1, 1, "No.", style = 1)}${cellStr(1, 2, "Photo", style = 1)}</row>")

        for (n in 1..count) {
            val r = n + 1
            val item = byNumber[n]
            val heightPt = if (item != null) item.heightPx * 0.75 + 8 else 15.0
            rows.append("<row r=\"$r\" ht=\"$heightPt\" customHeight=\"1\">${cellInt(r, 1, n)}</row>")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<cols>
<col min="1" max="1" width="6" customWidth="1"/>
<col min="2" max="2" width="24" customWidth="1"/>
</cols>
<sheetData>
$rows
</sheetData>
<drawing r:id="rId1"/>
</worksheet>"""
    }

    private fun buildDrawingXml(photoItems: List<PhotoItem>): String {
        val anchors = StringBuilder()
        for ((i, item) in photoItems.withIndex()) {
            val picId = i + 1
            val relId = i + 1
            val rowIndex0 = item.number // row (n+1) is 1-based; 0-based == n == item.number
            val cx = pxToEmu(item.widthPx)
            val cy = pxToEmu(item.heightPx)
            anchors.append(
                "<xdr:oneCellAnchor>" +
                    "<xdr:from><xdr:col>1</xdr:col><xdr:colOff>19050</xdr:colOff>" +
                    "<xdr:row>$rowIndex0</xdr:row><xdr:rowOff>19050</xdr:rowOff></xdr:from>" +
                    "<xdr:ext cx=\"$cx\" cy=\"$cy\"/>" +
                    "<xdr:pic>" +
                    "<xdr:nvPicPr><xdr:cNvPr id=\"$picId\" name=\"Image$picId\"/><xdr:cNvPicPr/></xdr:nvPicPr>" +
                    "<xdr:blipFill><a:blip r:embed=\"rId$relId\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>" +
                    "<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"$cx\" cy=\"$cy\"/></a:xfrm>" +
                    "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr>" +
                    "</xdr:pic>" +
                    "<xdr:clientData/>" +
                    "</xdr:oneCellAnchor>"
            )
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
$anchors
</xdr:wsDr>"""
    }

    private fun buildDrawingRelsXml(photoItems: List<PhotoItem>): String {
        val rels = StringBuilder()
        for (i in photoItems.indices) {
            val relId = i + 1
            rels.append(
                "<Relationship Id=\"rId$relId\" " +
                    "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" " +
                    "Target=\"../media/image$relId.jpg\"/>"
            )
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
</Relationships>"""
    }

    private val SHEET2_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/>
</Relationships>"""

    private val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="jpg" ContentType="image/jpeg"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
<Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>
</Types>"""

    private val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private val WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>
<sheet name="Report" sheetId="1" r:id="rId1"/>
<sheet name="Photos" sheetId="2" r:id="rId3"/>
</sheets>
</workbook>"""

    private val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
</Relationships>"""

    private val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1">
<numFmt numFmtId="164" formatCode="&quot;${'$'}&quot;#,##0.00"/>
</numFmts>
<fonts count="3">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="14"/><name val="Calibri"/></font>
</fonts>
<fills count="2">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
</fills>
<borders count="1">
<border><left/><right/><top/><bottom/><diagonal/></border>
</borders>
<cellStyleXfs count="1">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
</cellStyleXfs>
<cellXfs count="4">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
</cellXfs>
<cellStyles count="1">
<cellStyle name="Normal" xfId="0" builtinId="0"/>
</cellStyles>
</styleSheet>"""
}
