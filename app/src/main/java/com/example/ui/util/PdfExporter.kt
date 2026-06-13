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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 postscript points

        // Filter transactions for specified month and year
        val filteredTx = transactions.filter { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.date
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }.sortedBy { it.date } // Mutasi is usually sorted ascendingly by date (oldest to newest) to match bank records!

        val totalIncome = filteredTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = filteredTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense

        val monthName = getMonthNameIndonesian(month)
        val periodString = "$monthName $year"

        // Paints
        val paintTitle = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Deep primary blue
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = Color.parseColor("#475569") // Slate regular
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintHeaderLabel = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBody = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintGreen = Paint().apply {
            color = Color.parseColor("#15803D") // Forest Green
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintRed = Paint().apply {
            color = Color.parseColor("#B91C1C") // Crimson Red
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.parseColor("#CBD5E1") // Slate light border
            style = Paint.Style.STROKE
            strokeWidth = 1f
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
            // Draw Main Title
            can.drawText("DUITKU - LAPORAN MUTASI BULANAN", 40f, 50f, paintTitle)
            
            // Draw subtitle info
            can.drawText("Laporan Ringkasan Keuangan Personal & Mutasi Aliran Dana", 40f, 65f, paintSubtitle)
            
            // Draw visual dividing line
            can.drawLine(40f, 75f, 555f, 75f, paintLine)

            // Period and Metadata block
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
            can.drawText("Periode Laporan: $periodString", 40f, 95f, paintHeaderLabel)
            can.drawText("Dicetak Pada: ${sdf.format(Date())}", 40f, 110f, paintSubtitle)
            
            // Draw Page indicator at first line
            val pageStr = "Halaman $pageNum"
            val textWidth = paintSubtitle.measureText(pageStr)
            can.drawText(pageStr, 555f - textWidth, 95f, paintSubtitle)
        }

        drawPageHeader(canvas, currentPageNumber)
        yPos = 135f

        // Draw Summary Cards on first page
        // Three boxes: Total Income, Total Expense, Net Savings
        // Left offset = 40f. Available space = 515f. Width per box = 160f. GAP = 15f.
        val boxWidth = 160f
        val gap = 15f
        val boxHeight = 65f

        // Box 1: Pemasukan
        canvas.drawRect(40f, yPos, 40f + boxWidth, yPos + boxHeight, paintGreyBg)
        canvas.drawRect(40f, yPos, 40f + boxWidth, yPos + boxHeight, paintLine)
        canvas.drawText("TOTAL PEMASUKAN", 48f, yPos + 18f, paintSubtitle)
        canvas.drawText(viewModel.formatRupiah(totalIncome), 48f, yPos + 48f, paintGreen)

        // Box 2: Pengeluaran
        val b2X = 40f + boxWidth + gap
        canvas.drawRect(b2X, yPos, b2X + boxWidth, yPos + boxHeight, paintGreyBg)
        canvas.drawRect(b2X, yPos, b2X + boxWidth, yPos + boxHeight, paintLine)
        canvas.drawText("TOTAL PENGELUARAN", b2X + 8f, yPos + 18f, paintSubtitle)
        canvas.drawText(viewModel.formatRupiah(totalExpense), b2X + 8f, yPos + 48f, paintRed)

        // Box 3: Selisih (Surplus/Defisit)
        val b3X = b2X + boxWidth + gap
        canvas.drawRect(b3X, yPos, b3X + boxWidth, yPos + boxHeight, paintGreyBg)
        canvas.drawRect(b3X, yPos, b3X + boxWidth, yPos + boxHeight, paintLine)
        canvas.drawText("SELISIH (NET)", b3X + 8f, yPos + 18f, paintSubtitle)
        val finalPaint = if (netSavings >= 0) paintGreen else paintRed
        canvas.drawText(viewModel.formatRupiah(netSavings), b3X + 8f, yPos + 48f, finalPaint)

        yPos += boxHeight + 35f

        // Draw Transaction List Table Title
        canvas.drawText("RINCIAN MUTASI TRANSAKSI", 40f, yPos, paintHeaderLabel)
        yPos += 12f

        // Draw Table Header Background and labels
        val colX_tanggal = 40f
        val colX_dompet = 100f
        val colX_kategori = 180f
        val colX_keterangan = 270f
        val colX_jumlah = 555f // right aligned endpoint, drawText from right requires alignment

        fun drawTableHeaderRow(can: Canvas, y: Float) {
            can.drawRect(40f, y, 555f, y + 22f, paintGreyBg)
            can.drawRect(40f, y, 555f, y + 22f, paintLine)
            
            can.drawText("Tanggal", colX_tanggal + 6f, y + 15f, paintHeaderLabel)
            can.drawText("Akun/Dompet", colX_dompet + 6f, y + 15f, paintHeaderLabel)
            can.drawText("Kategori", colX_kategori + 6f, y + 15f, paintHeaderLabel)
            can.drawText("Keterangan", colX_keterangan + 6f, y + 15f, paintHeaderLabel)
            
            // Right-aligned header
            val rightAlignPaint = Paint(paintHeaderLabel).apply { textAlign = Paint.Align.RIGHT }
            can.drawText("Jumlah", colX_jumlah - 6f, y + 15f, rightAlignPaint)
        }

        drawTableHeaderRow(canvas, yPos)
        yPos += 22f

        // Draw table rows
        val rowHeight = 24f
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

        if (filteredTx.isEmpty()) {
            canvas.drawRect(40f, yPos, 555f, yPos + rowHeight * 2, paintLine)
            val noTxPaint = Paint(paintSubtitle).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("Tidak ada riwayat mutasi transaksi di periode ini.", 595f / 2f, yPos + rowHeight + 5f, noTxPaint)
            yPos += rowHeight * 2
        } else {
            for (tx in filteredTx) {
                // If yPos gets too close to the bottom of the page, cycle page
                if (yPos > 760f) {
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawPageHeader(canvas, currentPageNumber)
                    yPos = 135f
                    drawTableHeaderRow(canvas, yPos)
                    yPos += 22f
                }

                // Collect names
                val walletName = wallets.firstOrNull { it.id == tx.walletId }?.name ?: "Unknown"
                val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Lain-lain"
                val dateStr = sdfDate.format(Date(tx.date))
                
                // Truncate strings if too long for column widths
                val displayKeterangan = if (tx.note.length > 22) tx.note.take(20) + ".." else tx.note.ifEmpty { "-" }
                val displayWallet = if (walletName.length > 12) walletName.take(10) + ".." else walletName
                val displayKategori = if (catName.length > 14) catName.take(12) + ".." else catName

                // Draw row outline (bottom line)
                canvas.drawLine(40f, yPos + rowHeight, 555f, yPos + rowHeight, paintLine)
                
                // Draw cells
                canvas.drawText(dateStr, colX_tanggal + 6f, yPos + 16f, paintBody)
                canvas.drawText(displayWallet, colX_dompet + 6f, yPos + 16f, paintBody)
                canvas.drawText(displayKategori, colX_kategori + 6f, yPos + 16f, paintBody)
                canvas.drawText(displayKeterangan, colX_keterangan + 6f, yPos + 16f, paintBody)

                // Sign and amount formatting (+/-)
                val isIncome = tx.type == "INCOME"
                val amtStr = (if (isIncome) "+ " else "- ") + viewModel.formatRupiah(tx.amount)
                val valuePaint = if (isIncome) paintGreen else paintRed

                // Draw amount right-aligned
                val valRightPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(amtStr, colX_jumlah - 6f, yPos + 16f, valRightPaint)

                yPos += rowHeight
            }
        }

        // Draw fine print footer on last page
        if (yPos > 720f) {
            // Need a new page for footer
            pdfDocument.finishPage(page)
            currentPageNumber++
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawPageHeader(canvas, currentPageNumber)
            yPos = 135f
        }

        yPos += 30f
        canvas.drawLine(40f, yPos, 555f, yPos, paintLine)
        yPos += 15f

        val paintFooterText = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("App Disclaimer: Seluruh perhitungan di atas disimpan secara lokal di perangkat Anda melalui database internal DuitKu.", 40f, yPos, paintFooterText)
        canvas.drawText("Silakan simpan fail PDF ini sebagai rujukan mutasi rekening atau cetak fisik dokumen bila diperlukan.", 40f, yPos + 12f, paintFooterText)

        pdfDocument.finishPage(page)

        // Write to outputStream
        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun getMonthNameIndonesian(month: Int): String {
        return when (month) {
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
    }
}
