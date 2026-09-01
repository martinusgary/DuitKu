package com.example.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.viewmodel.FinanceViewModel
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    fun generateMonthlyPdfReport(
        context: Context,
        outputStream: OutputStream,
        month: Int,      // 0-indexed (0 = Jan, 11 = Dec)
        year: Int,
        transactions: List<Transaction>,
        wallets: List<Wallet>,
        categories: List<Category>,
        viewModel: FinanceViewModel
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt

        // Filter transactions for specified month and year
        val filteredTx = transactions.filter { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.date
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }.sortedBy { it.date }

        val totalIncome = filteredTx.filter { it.type == "INCOME" }.sumOf { (it.amount - it.adminFee).coerceAtLeast(0.0) }
        val totalExpense = filteredTx.filter { it.type == "EXPENSE" }.sumOf { it.amount + it.adminFee }
        val netSavings = totalIncome - totalExpense

        val appLanguage = viewModel.appLanguage.value
        val isIndonesian = appLanguage == "id"

        val monthName = getMonthName(month, isIndonesian)
        val periodString = "$monthName $year"

        // Localized text strings
        val titleStr = if (isIndonesian) "DUITKU - LAPORAN MUTASI BULANAN" else "DUITKU - MONTHLY TRANSACTION MUTATION REPORT"
        val subtitleStr = if (isIndonesian) "Laporan Ringkasan Keuangan Personal & Mutasi Aliran Dana" else "Personal Financial Summary & Cash Flow Mutation Report"
        val periodLbl = if (isIndonesian) "Periode Laporan" else "Report Period"
        val printedLbl = if (isIndonesian) "Dicetak Pada" else "Printed On"
        val pageLbl = if (isIndonesian) "Halaman" else "Page"
        
        val totalIncomeLbl = if (isIndonesian) "TOTAL PEMASUKAN" else "TOTAL INCOME"
        val totalExpenseLbl = if (isIndonesian) "TOTAL PENGELUARAN" else "TOTAL EXPENSE"
        val netSavingsLbl = if (isIndonesian) "SELISIH (NET)" else "NET DIFFERENCE"
        
        val tableTitleLbl = if (isIndonesian) "RINCIAN MUTASI TRANSAKSI" else "TRANSACTION MUTATION DETAILS"
        
        val colDateLbl = if (isIndonesian) "Tanggal" else "Date"
        val colWalletLbl = if (isIndonesian) "Akun/Dompet" else "Account/Wallet"
        val colCategoryLbl = if (isIndonesian) "Kategori" else "Category"
        val colNoteLbl = if (isIndonesian) "Keterangan" else "Description"
        val colAmountLbl = if (isIndonesian) "Jumlah & Admin" else "Amount & Fee"
        
        val emptyMessage = if (isIndonesian) "Tidak ada riwayat mutasi transaksi di periode ini." else "No transaction mutation history found in this period."
        val footerDisclaimer1 = if (isIndonesian) {
            "App Disclaimer: Seluruh perhitungan di atas disimpan secara lokal di perangkat Anda melalui database internal DuitKu."
        } else {
            "App Disclaimer: All calculations above are stored locally on your device in the DuitKu internal database."
        }
        val footerDisclaimer2 = if (isIndonesian) {
            "Silakan simpan fail PDF ini sebagai rujukan mutasi rekening atau cetak fisik dokumen bila diperlukan."
        } else {
            "Please save this PDF file as a reference for account mutations or print the document physically if needed."
        }

        // Paints
        val paintTitle = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Deep primary blue
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = Color.parseColor("#475569") // Slate regular
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintHeaderLabel = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBody = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintAdminSubtext = Paint().apply {
            color = Color.parseColor("#64748B") // Slate secondary
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val paintGreen = Paint().apply {
            color = Color.parseColor("#15803D") // Forest Green
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintRed = Paint().apply {
            color = Color.parseColor("#B91C1C") // Crimson Red
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBlue = Paint().apply {
            color = Color.parseColor("#2563EB") // Blue for transfer
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.parseColor("#CBD5E1") // Slate light border
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val paintGreyBg = Paint().apply {
            color = Color.parseColor("#F1F5F9") // Light table header / summary background
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Initialize First Page
        var currentPageNumber = 1
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Coordinate tracker
        var yPos = 50f

        // Draw header of report
        fun drawPageHeader(can: Canvas, pageNum: Int) {
            can.drawText(titleStr, 40f, 50f, paintTitle)
            can.drawText(subtitleStr, 40f, 65f, paintSubtitle)
            can.drawLine(40f, 75f, 555f, 75f, paintLine)

            val locale = if (isIndonesian) Locale("id", "ID") else Locale.US
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale)
            can.drawText("$periodLbl: $periodString", 40f, 95f, paintHeaderLabel)
            can.drawText("$printedLbl: ${sdf.format(Date())}", 40f, 110f, paintSubtitle)
            
            val pageStr = "$pageLbl $pageNum"
            val textWidth = paintSubtitle.measureText(pageStr)
            can.drawText(pageStr, 555f - textWidth, 95f, paintSubtitle)
        }

        drawPageHeader(canvas, currentPageNumber)
        yPos = 130f

        // Summary Cards
        val boxWidth = 160f
        val gap = 15f
        val boxHeight = 55f

        // Box 1: Pemasukan
        canvas.drawRect(40f, yPos, 40f + boxWidth, yPos + boxHeight, paintGreyBg)
        canvas.drawRect(40f, yPos, 40f + boxWidth, yPos + boxHeight, paintLine)
        canvas.drawText(totalIncomeLbl, 48f, yPos + 18f, paintSubtitle)
        canvas.drawText(viewModel.formatRupiah(totalIncome), 48f, yPos + 40f, paintGreen)

        // Box 2: Pengeluaran
        val b2X = 40f + boxWidth + gap
        canvas.drawRect(b2X, yPos, b2X + boxWidth, yPos + boxHeight, paintGreyBg)
        canvas.drawRect(b2X, yPos, b2X + boxWidth, yPos + boxHeight, paintLine)
        canvas.drawText(totalExpenseLbl, b2X + 8f, yPos + 18f, paintSubtitle)
        canvas.drawText(viewModel.formatRupiah(totalExpense), b2X + 8f, yPos + 40f, paintRed)

        // Box 3: Selisih (Surplus/Defisit)
        val b3X = b2X + boxWidth + gap
        canvas.drawRect(b3X, yPos, b3X + boxWidth, yPos + boxHeight, paintGreyBg)
        canvas.drawRect(b3X, yPos, b3X + boxWidth, yPos + boxHeight, paintLine)
        canvas.drawText(netSavingsLbl, b3X + 8f, yPos + 18f, paintSubtitle)
        val finalPaint = if (netSavings >= 0) paintGreen else paintRed
        canvas.drawText(viewModel.formatRupiah(netSavings), b3X + 8f, yPos + 40f, finalPaint)

        yPos += boxHeight + 30f

        // Transaction List Table Title
        canvas.drawText(tableTitleLbl, 40f, yPos, paintHeaderLabel)
        yPos += 12f

        // Table Column X coordinates (A4 width 595, margins: 40f to 555f = 515f width)
        val colX_tanggal = 40f     // width 55f (40 .. 95)
        val colX_dompet = 95f      // width 70f (95 .. 165)
        val colX_kategori = 165f   // width 110f (165 .. 275) - made wider for full category names
        val colX_keterangan = 275f // width 165f (275 .. 440) - wide and wraps multi-line cleanly
        val colX_jumlah = 555f     // right aligned endpoint at 555f (440 .. 555)

        val widthDompet = 65f
        val widthKategori = 105f
        val widthKeterangan = 160f

        fun drawTableHeaderRow(can: Canvas, y: Float) {
            can.drawRect(40f, y, 555f, y + 22f, paintGreyBg)
            can.drawRect(40f, y, 555f, y + 22f, paintLine)
            
            can.drawText(colDateLbl, colX_tanggal + 4f, y + 15f, paintHeaderLabel)
            can.drawText(colWalletLbl, colX_dompet + 4f, y + 15f, paintHeaderLabel)
            can.drawText(colCategoryLbl, colX_kategori + 4f, y + 15f, paintHeaderLabel)
            can.drawText(colNoteLbl, colX_keterangan + 4f, y + 15f, paintHeaderLabel)
            
            val rightAlignPaint = Paint(paintHeaderLabel).apply { textAlign = Paint.Align.RIGHT }
            can.drawText(colAmountLbl, colX_jumlah - 4f, y + 15f, rightAlignPaint)
        }

        drawTableHeaderRow(canvas, yPos)
        yPos += 22f

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", if (isIndonesian) Locale("id", "ID") else Locale.US)

        if (filteredTx.isEmpty()) {
            canvas.drawRect(40f, yPos, 555f, yPos + 44f, paintLine)
            val noTxPaint = Paint(paintSubtitle).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(emptyMessage, 595f / 2f, yPos + 26f, noTxPaint)
            yPos += 44f
        } else {
            for (tx in filteredTx) {
                // Collect Names
                val walletName = wallets.firstOrNull { it.id == tx.walletId }?.name ?: "Dompet ${tx.walletId}"
                val targetWalletName = tx.targetWalletId?.let { tId -> wallets.firstOrNull { it.id == tId }?.name ?: "Dompet $tId" }
                val catName = if (tx.type == "TRANSFER") {
                    if (isIndonesian) "Transfer Saldo" else "Fund Transfer"
                } else {
                    categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Lain-lain"
                }
                val dateStr = sdfDate.format(Date(tx.date))
                
                // Description note
                val noteText = when {
                    tx.type == "TRANSFER" && targetWalletName != null -> {
                        val base = if (isIndonesian) "Transfer ke $targetWalletName" else "Transfer to $targetWalletName"
                        if (tx.note.isNotBlank()) "$base (${tx.note})" else base
                    }
                    tx.note.isNotBlank() -> tx.note
                    else -> "-"
                }

                // Wrap text for columns that might have longer content
                val walletLines = splitTextIntoLines(walletName, widthDompet, paintBody)
                val categoryLines = splitTextIntoLines(catName, widthKategori, paintBody)
                val noteLines = splitTextIntoLines(noteText, widthKeterangan, paintBody)

                val hasAdminFee = tx.adminFee > 0.0
                val amountLinesCount = if (hasAdminFee) 2 else 1

                val maxLines = maxOf(walletLines.size, categoryLines.size, noteLines.size, amountLinesCount)
                val dynamicRowHeight = maxOf(24f, 10f + maxLines * 12f)

                // Pagination check
                if (yPos + dynamicRowHeight > 760f) {
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawPageHeader(canvas, currentPageNumber)
                    yPos = 130f
                    drawTableHeaderRow(canvas, yPos)
                    yPos += 22f
                }

                // Draw row outline (bottom line)
                canvas.drawLine(40f, yPos + dynamicRowHeight, 555f, yPos + dynamicRowHeight, paintLine)

                // 1. Draw Date
                canvas.drawText(dateStr, colX_tanggal + 4f, yPos + 14f, paintBody)

                // 2. Draw Wallet Name lines
                walletLines.forEachIndexed { idx, line ->
                    canvas.drawText(line, colX_dompet + 4f, yPos + 14f + (idx * 11f), paintBody)
                }

                // 3. Draw Category Name lines (full text)
                categoryLines.forEachIndexed { idx, line ->
                    canvas.drawText(line, colX_kategori + 4f, yPos + 14f + (idx * 11f), paintBody)
                }

                // 4. Draw Full Description Note lines (full text wrapped)
                noteLines.forEachIndexed { idx, line ->
                    canvas.drawText(line, colX_keterangan + 4f, yPos + 14f + (idx * 11f), paintBody)
                }

                // 5. Draw Amount & Transparent Admin Fee
                val (amtSign, valuePaint) = when (tx.type) {
                    "INCOME" -> "+ " to paintGreen
                    "TRANSFER" -> "⇄ " to paintBlue
                    else -> "- " to paintRed
                }
                val amtStr = amtSign + viewModel.formatRupiah(tx.amount)

                val valRightPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(amtStr, colX_jumlah - 4f, yPos + 14f, valRightPaint)

                // Draw Admin Fee Subtext if applicable
                if (hasAdminFee) {
                    val feeStr = if (isIndonesian) {
                        "(Admin: +${viewModel.formatRupiah(tx.adminFee)})"
                    } else {
                        "(Fee: +${viewModel.formatRupiah(tx.adminFee)})"
                    }
                    val feeRightPaint = Paint(paintAdminSubtext).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText(feeStr, colX_jumlah - 4f, yPos + 25f, feeRightPaint)
                }

                yPos += dynamicRowHeight
            }
        }

        // Draw fine print footer on last page
        if (yPos > 720f) {
            pdfDocument.finishPage(page)
            currentPageNumber++
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawPageHeader(canvas, currentPageNumber)
            yPos = 130f
        }

        yPos += 25f
        canvas.drawLine(40f, yPos, 555f, yPos, paintLine)
        yPos += 15f

        val paintFooterText = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText(footerDisclaimer1, 40f, yPos, paintFooterText)
        canvas.drawText(footerDisclaimer2, 40f, yPos + 11f, paintFooterText)

        pdfDocument.finishPage(page)

        // Write to outputStream
        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun splitTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == "-") return listOf("-")
        
        if (paint.measureText(trimmed) <= maxWidth) {
            return listOf(trimmed)
        }

        val words = trimmed.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                // If the single word itself exceeds maxWidth, split word characters
                if (paint.measureText(word) > maxWidth) {
                    var subWord = ""
                    for (ch in word) {
                        if (paint.measureText(subWord + ch) <= maxWidth) {
                            subWord += ch
                        } else {
                            if (subWord.isNotEmpty()) lines.add(subWord)
                            subWord = ch.toString()
                        }
                    }
                    if (subWord.isNotEmpty()) {
                        currentLine = StringBuilder(subWord)
                    }
                } else {
                    currentLine = StringBuilder(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines.ifEmpty { listOf(trimmed) }
    }

    private fun getMonthName(month: Int, isIndonesian: Boolean): String {
        return if (isIndonesian) {
            when (month) {
                Calendar.JANUARY -> "Januari"
                Calendar.FEBRUARY -> "Februari"
                Calendar.MARCH -> "Maret"
                Calendar.APRIL -> "April"
                Calendar.MAY -> "Mei"
                Calendar.JUNE -> "Juni"
                Calendar.JULY -> "Juli"
                Calendar.AUGUST -> "Agustus"
                Calendar.SEPTEMBER -> "September"
                Calendar.OCTOBER -> "Oktober"
                Calendar.NOVEMBER -> "November"
                Calendar.DECEMBER -> "Desember"
                else -> "Bulan"
            }
        } else {
            when (month) {
                Calendar.JANUARY -> "January"
                Calendar.FEBRUARY -> "February"
                Calendar.MARCH -> "March"
                Calendar.APRIL -> "April"
                Calendar.MAY -> "May"
                Calendar.JUNE -> "June"
                Calendar.JULY -> "July"
                Calendar.AUGUST -> "August"
                Calendar.SEPTEMBER -> "September"
                Calendar.OCTOBER -> "October"
                Calendar.NOVEMBER -> "November"
                Calendar.DECEMBER -> "December"
                else -> "Month"
            }
        }
    }
}
