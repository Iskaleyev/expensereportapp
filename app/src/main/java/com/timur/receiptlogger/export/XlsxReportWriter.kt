package com.timur.receiptlogger.export

import com.timur.receiptlogger.data.ReceiptEntry
import com.timur.receiptlogger.data.Trip
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal, hand-built .xlsx (OOXML spreadsheet) file, with no external library.
 * Only the small subset of the format this report needs: inline strings, numeric cells,
 * a few cell styles (bold text, currency number format), and fixed column widths.
 *
 * This exact XML structure was prototyped and round-trip verified (openpyxl + LibreOffice)
 * before being ported here.
 */
object XlsxReportWriter {

    fun writeTripReport(outputFile: File, trip: Trip, receipts: List<ReceiptEntry>) {
        val dateFmt = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val dateRange = "${dateFmt.format(Date(trip.startDate))} – ${dateFmt.format(Date(trip.endDate))}"
        val total = receipts.sumOf { it.amount }

        val sheetXml = buildSheetXml(trip.name, dateRange, receipts, total, dateFmt)

        outputFile.parentFile?.mkdirs()
        ZipOutputStream(outputFile.outputStream()).use { zip ->
            writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
            writeEntry(zip, "_rels/.rels", RELS)
            writeEntry(zip, "xl/workbook.xml", WORKBOOK)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            writeEntry(zip, "xl/styles.xml", STYLES)
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
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

    private fun buildSheetXml(
        tripName: String,
        dateRange: String,
        receipts: List<ReceiptEntry>,
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
            "<row r=\"$headerRow\">${cellStr(headerRow, 1, "Date", style = 1)}" +
                "${cellStr(headerRow, 2, "Amount", style = 1)}${cellStr(headerRow, 3, "Note", style = 1)}</row>"
        )
        r++

        for (entry in receipts.sortedBy { it.timestamp }) {
            val dateText = dateFmt.format(Date(entry.timestamp))
            rows.append(
                "<row r=\"$r\">${cellStr(r, 1, dateText)}${cellNum(r, 2, entry.amount, style = 2)}" +
                    "${cellStr(r, 3, entry.note)}</row>"
            )
            r++
        }

        r++ // blank row
        rows.append("<row r=\"$r\">${cellStr(r, 1, "Total", style = 1)}${cellNum(r, 2, total, style = 2)}</row>")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols>
<col min="1" max="1" width="14" customWidth="1"/>
<col min="2" max="2" width="12" customWidth="1"/>
<col min="3" max="3" width="40" customWidth="1"/>
</cols>
<sheetData>
$rows
</sheetData>
</worksheet>"""
    }

    private val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private val WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>
<sheet name="Report" sheetId="1" r:id="rId1"/>
</sheets>
</workbook>"""

    private val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
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
