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

    private data class FormattedTxRow(
        val dateStr: String,
        val walletLines: List<String>,
        val categoryLines: List<String>,
        val noteLines: List<String>,
        val amountStr: String,
        val amountColor: Int,
        val adminFeeStr: String?,
        val rowHeight: Float
    )

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
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        val appLanguage = viewModel.appLanguage.value
        val isIndonesian = appLanguage == "id"

        // 1. Calculate Period Boundaries
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonthMs = startCal.timeInMillis

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfMonthMs = endCal.timeInMillis

        // Filter transactions for this month
        val monthlyTx = transactions.filter { tx ->
            tx.date in startOfMonthMs..endOfMonthMs
        }.sortedBy { it.date }

        // 2. Accurate Calculation of Saldo Awal (Initial Balance) and Summary Box
        val currentTotalBalance = wallets.sumOf { it.balance }

        var netChangeSinceStart = 0.0
        for (tx in transactions) {
            if (tx.date >= startOfMonthMs) {
                when (tx.type) {
                    "INCOME" -> netChangeSinceStart += (tx.amount - tx.adminFee).coerceAtLeast(0.0)
                    "EXPENSE" -> netChangeSinceStart -= (tx.amount + tx.adminFee)
                    "TRANSFER" -> netChangeSinceStart -= tx.adminFee
                }
            }
        }

        val calculatedInitialBalance = (currentTotalBalance - netChangeSinceStart).coerceAtLeast(0.0)
        val totalIncome = monthlyTx.filter { it.type == "INCOME" }.sumOf { (it.amount - it.adminFee).coerceAtLeast(0.0) }
        val totalExpense = monthlyTx.filter { it.type == "EXPENSE" }.sumOf { it.amount + it.adminFee }
        val totalTransferAdminFees = monthlyTx.filter { it.type == "TRANSFER" }.sumOf { it.adminFee }
        val finalClosingBalance = (calculatedInitialBalance + totalIncome - totalExpense - totalTransferAdminFees).coerceAtLeast(0.0)

        val monthName = getMonthName(month, isIndonesian)
        val periodDisplay = "$monthName $year"

        // Localized Strings
        val titleStr = if (isIndonesian) "DUITKU - LAPORAN MUTASI BULANAN" else "DUITKU - MONTHLY TRANSACTION MUTATION REPORT"
        val subtitleStr = if (isIndonesian) "Laporan Ringkasan Keuangan Personal & Mutasi Aliran Dana" else "Personal Financial Summary & Cash Flow Mutation Report"
        val reportPeriodLbl = if (isIndonesian) "Periode Laporan" else "Report Period"
        val printedOnLbl = if (isIndonesian) "Dicetak Pada" else "Printed On"
        val pageLbl = if (isIndonesian) "Halaman" else "Page"

        val initialBalanceTitle = if (isIndonesian) "SALDO AWAL" else "INITIAL BALANCE"
        val totalIncomeTitle = if (isIndonesian) "TOTAL PEMASUKAN" else "TOTAL INCOME"
        val totalExpenseTitle = if (isIndonesian) "TOTAL PENGELUARAN" else "TOTAL EXPENSE"
        val closingBalanceTitle = if (isIndonesian) "SALDO AKHIR" else "CLOSING BALANCE"

        val tableTitle = if (isIndonesian) "RINCIAN MUTASI TRANSAKSI" else "TRANSACTION MUTATION DETAILS"
        val colDateLbl = if (isIndonesian) "Tanggal" else "Date"
        val colWalletLbl = if (isIndonesian) "Akun/Dompet" else "Account/Wallet"
        val colCategoryLbl = if (isIndonesian) "Kategori" else "Category"
        val colDescLbl = if (isIndonesian) "Keterangan" else "Description"
        val colAmountLbl = if (isIndonesian) "Jumlah" else "Amount"

        val emptyMessage = if (isIndonesian) "Tidak ada riwayat mutasi transaksi di periode ini." else "No transaction mutation history found in this period."
        val footerDisclaimer1 = if (isIndonesian) {
            "App Disclaimer: Seluruh perhitungan di atas disimpan secara lokal di perangkat Anda melalui database internal DuitKu."
        } else {
            "App Disclaimer: All calculations above are stored locally on your device in the DuitKu internal database."
        }
        val footerDisclaimer2 = if (isIndonesian) {
            "Silakan simpan file PDF ini sebagai rujukan mutasi rekening atau cetak fisik dokumen bila diperlukan."
        } else {
            "Please save this PDF file as a reference for account mutations or print the document physically if needed."
        }

        val sansNormal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val sansItalic = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

        fun createPdfPaint(tf: Typeface, size: Float, clr: Int, spacing: Float = 0.02f): Paint {
            return Paint().apply {
                typeface = tf
                textSize = size
                color = clr
                isAntiAlias = true
                isSubpixelText = true
                isLinearText = true
                letterSpacing = spacing
            }
        }

        // Paints Setup (Clean Light Theme with Roboto/SansSerif and crisp letter spacing)
        val paintTitle = createPdfPaint(sansBold, 13f, Color.parseColor("#1E3A8A"), 0.015f)
        val paintSubtitle = createPdfPaint(sansNormal, 8.5f, Color.parseColor("#64748B"), 0.015f)
        val paintMetaBold = createPdfPaint(sansBold, 8.5f, Color.parseColor("#1E293B"), 0.015f)
        val paintMetaRegular = createPdfPaint(sansNormal, 8.5f, Color.parseColor("#475569"), 0.015f)
        val paintSectionTitle = createPdfPaint(sansBold, 9f, Color.parseColor("#1E293B"), 0.015f)

        // Summary Card Paints
        val paintCardBg = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintCardBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val paintCardLabel = createPdfPaint(sansBold, 7f, Color.parseColor("#64748B"), 0.02f)
        val paintCardInitialVal = createPdfPaint(sansBold, 8.5f, Color.parseColor("#0F172A"), 0.015f)
        val paintCardIncomeVal = createPdfPaint(sansBold, 8.5f, Color.parseColor("#15803D"), 0.015f)
        val paintCardExpenseVal = createPdfPaint(sansBold, 8.5f, Color.parseColor("#B91C1C"), 0.015f)
        val paintCardClosingVal = createPdfPaint(sansBold, 8.5f, Color.parseColor("#2563EB"), 0.015f)

        // Table Paints
        val paintTableHeaderText = createPdfPaint(sansBold, 8f, Color.parseColor("#1E293B"), 0.02f)

        val paintTableLine = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            isAntiAlias = true
        }

        val paintTableDivider = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
            isAntiAlias = true
        }

        val paintBody = createPdfPaint(sansNormal, 8f, Color.parseColor("#334155"), 0.02f)
        val paintAdminSubtext = createPdfPaint(sansItalic, 7f, Color.parseColor("#64748B"), 0.02f)

        val paintFooterText = createPdfPaint(sansItalic, 7.5f, Color.parseColor("#64748B"), 0.02f)

        // Table Column X Coordinates (Page width = 595, margins: 38f to 557f = 519f printable width)
        val colX_Date = 38f         // width ~50f (38..88)
        val colX_Wallet = 94f       // width ~62f (94..156)
        val colX_Category = 162f    // width ~82f (162..244)
        val colX_Desc = 250f        // width ~158f (250..408)
        val colX_Amount = 557f      // right aligned at 557f (ample safe zone > 140f)

        val widthWallet = 60f
        val widthCategory = 80f
        val widthDesc = 154f

        // Precompute formatted rows
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", if (isIndonesian) Locale("id", "ID") else Locale.US)
        val formattedRows = mutableListOf<FormattedTxRow>()

        for (tx in monthlyTx) {
            val walletName = wallets.firstOrNull { it.id == tx.walletId }?.name ?: "Dompet ${tx.walletId}"
            val targetWalletName = tx.targetWalletId?.let { tId -> wallets.firstOrNull { it.id == tId }?.name ?: "Dompet $tId" }
            val catName = if (tx.type == "TRANSFER") {
                if (isIndonesian) "Transfer Saldo" else "Fund Transfer"
            } else {
                categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Lain-lain"
            }

            val dateStr = sdfDate.format(Date(tx.date))

            val noteText = when {
                tx.type == "TRANSFER" && targetWalletName != null -> {
                    val base = if (isIndonesian) "Transfer ke $targetWalletName" else "Transfer to $targetWalletName"
                    if (tx.note.isNotBlank()) "$base (${tx.note})" else base
                }
                tx.note.isNotBlank() -> tx.note
                else -> "-"
            }

            val walletLines = splitTextIntoLines(walletName, widthWallet, paintBody)
            val categoryLines = splitTextIntoLines(catName, widthCategory, paintBody)
            val noteLines = splitTextIntoLines(noteText, widthDesc, paintBody)

            val adminFeeStr = if (tx.adminFee > 0.0) {
                if (isIndonesian) "(Admin: +${viewModel.formatRupiah(tx.adminFee)})" else "(Fee: +${viewModel.formatRupiah(tx.adminFee)})"
            } else null

            val (amtSign, amtColor) = when (tx.type) {
                "INCOME" -> "+ " to Color.parseColor("#15803D")
                "TRANSFER" -> "⇄ " to Color.parseColor("#2563EB")
                else -> "- " to Color.parseColor("#B91C1C")
            }
            val amtStr = amtSign + viewModel.formatRupiah(tx.amount)

            val maxLineCount = maxOf(
                walletLines.size,
                categoryLines.size,
                noteLines.size,
                if (adminFeeStr != null) 2 else 1
            )
            // Generous line height (13.5f per line) prevents vertical font collision between lines and row borders
            val rowHeight = maxOf(24f, 9f + (maxLineCount * 13.5f))

            formattedRows.add(
                FormattedTxRow(
                    dateStr = dateStr,
                    walletLines = walletLines,
                    categoryLines = categoryLines,
                    noteLines = noteLines,
                    amountStr = amtStr,
                    amountColor = amtColor,
                    adminFeeStr = adminFeeStr,
                    rowHeight = rowHeight
                )
            )
        }

        // Pagination Partitioning
        val pagesRows = mutableListOf<MutableList<FormattedTxRow>>()
        var curPageList = mutableListOf<FormattedTxRow>()
        var curPageY = 250f
        var isFirstPage = true

        if (formattedRows.isEmpty()) {
            pagesRows.add(mutableListOf())
        } else {
            for (row in formattedRows) {
                val maxLimitY = if (isFirstPage) 730f else 750f
                if (curPageY + row.rowHeight > maxLimitY && curPageList.isNotEmpty()) {
                    pagesRows.add(curPageList)
                    curPageList = mutableListOf()
                    isFirstPage = false
                    curPageY = 120f
                }
                curPageList.add(row)
                curPageY += row.rowHeight
            }
            if (curPageList.isNotEmpty()) {
                pagesRows.add(curPageList)
            }
        }

        val totalPages = pagesRows.size
        val sdfNow = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", if (isIndonesian) Locale("id", "ID") else Locale.US)
        val printedTime = sdfNow.format(Date())

        // Header Drawer Function
        fun drawPageHeader(can: Canvas, pageNum: Int) {
            can.drawText(titleStr, 38f, 50f, paintTitle)
            can.drawText(subtitleStr, 38f, 64f, paintSubtitle)
            can.drawLine(38f, 74f, 557f, 74f, paintTableLine)

            can.drawText("$reportPeriodLbl: $periodDisplay", 38f, 92f, paintMetaBold)
            can.drawText("$printedOnLbl: $printedTime", 38f, 105f, paintMetaRegular)

            val pageStr = "$pageLbl $pageNum"
            val pRight = Paint(paintMetaRegular).apply { textAlign = Paint.Align.RIGHT }
            can.drawText(pageStr, 557f, 92f, pRight)
        }

        fun drawTableHeaderRow(can: Canvas, y: Float) {
            can.drawLine(38f, y, 557f, y, paintTableDivider)

            can.drawText(colDateLbl, colX_Date, y + 14f, paintTableHeaderText)
            can.drawText(colWalletLbl, colX_Wallet, y + 14f, paintTableHeaderText)
            can.drawText(colCategoryLbl, colX_Category, y + 14f, paintTableHeaderText)
            can.drawText(colDescLbl, colX_Desc, y + 14f, paintTableHeaderText)

            val rightAlign = Paint(paintTableHeaderText).apply { textAlign = Paint.Align.RIGHT }
            can.drawText(colAmountLbl, colX_Amount, y + 14f, rightAlign)

            can.drawLine(38f, y + 21f, 557f, y + 21f, paintTableDivider)
        }

        // Render each page
        for ((pIdx, pageRows) in pagesRows.withIndex()) {
            val pageNum = pIdx + 1
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPos: Float

            if (pageNum == 1) {
                drawPageHeader(canvas, pageNum)
                yPos = 125f

                // 4 Summary Mutation Cards (Saldo Awal, Total Pemasukan, Total Pengeluaran, Saldo Akhir)
                val totalAreaWidth = 519f
                val gap = 7f
                val cardWidth = (totalAreaWidth - (3 * gap)) / 4f
                val cardHeight = 46f

                val cardsData = listOf(
                    Triple(initialBalanceTitle, viewModel.formatRupiah(calculatedInitialBalance), paintCardInitialVal),
                    Triple(totalIncomeTitle, viewModel.formatRupiah(totalIncome), paintCardIncomeVal),
                    Triple(totalExpenseTitle, viewModel.formatRupiah(totalExpense), paintCardExpenseVal),
                    Triple(closingBalanceTitle, viewModel.formatRupiah(finalClosingBalance), paintCardClosingVal)
                )

                for ((cIdx, card) in cardsData.withIndex()) {
                    val cx = 38f + (cIdx * (cardWidth + gap))
                    canvas.drawRect(cx, yPos, cx + cardWidth, yPos + cardHeight, paintCardBg)
                    canvas.drawRect(cx, yPos, cx + cardWidth, yPos + cardHeight, paintCardBorder)

                    // Auto-scale value font if number is large to avoid overflowing borders
                    val vPaint = Paint(card.third)
                    while (vPaint.measureText(card.second) > (cardWidth - 14f) && vPaint.textSize > 6.0f) {
                        vPaint.textSize -= 0.4f
                    }

                    canvas.drawText(card.first, cx + 7f, yPos + 16f, paintCardLabel)
                    canvas.drawText(card.second, cx + 7f, yPos + 34f, vPaint)
                }

                yPos += cardHeight + 25f

                // Table Section Title
                canvas.drawText(tableTitle, 38f, yPos, paintSectionTitle)
                yPos += 10f

                drawTableHeaderRow(canvas, yPos)
                yPos += 21f
            } else {
                drawPageHeader(canvas, pageNum)
                yPos = 120f
                drawTableHeaderRow(canvas, yPos)
                yPos += 21f
            }

            // Draw Rows
            if (pageRows.isEmpty()) {
                val emptyPaint = Paint(paintSubtitle).apply { textAlign = Paint.Align.CENTER }
                canvas.drawText(emptyMessage, 595f / 2f, yPos + 22f, emptyPaint)
                yPos += 35f
                canvas.drawLine(38f, yPos, 557f, yPos, paintTableLine)
            } else {
                for (row in pageRows) {
                    val rTop = yPos
                    val rBottom = yPos + row.rowHeight

                    // Bottom row divider line
                    canvas.drawLine(38f, rBottom, 557f, rBottom, paintTableLine)

                    val textBaseY = rTop + 14f

                    // 1. Date
                    canvas.drawText(row.dateStr, colX_Date, textBaseY, paintBody)

                    // 2. Wallet lines
                    row.walletLines.forEachIndexed { idx, line ->
                        canvas.drawText(line, colX_Wallet, textBaseY + (idx * 13.5f), paintBody)
                    }

                    // 3. Category lines
                    row.categoryLines.forEachIndexed { idx, line ->
                        canvas.drawText(line, colX_Category, textBaseY + (idx * 13.5f), paintBody)
                    }

                    // 4. Description lines
                    row.noteLines.forEachIndexed { idx, line ->
                        canvas.drawText(line, colX_Desc, textBaseY + (idx * 13.5f), paintBody)
                    }

                    // 5. Amount & Admin
                    val amtPaint = createPdfPaint(sansBold, 8f, row.amountColor, 0.02f).apply {
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(row.amountStr, colX_Amount, textBaseY, amtPaint)

                    if (row.adminFeeStr != null) {
                        val feePaint = createPdfPaint(sansItalic, 7f, Color.parseColor("#64748B"), 0.02f).apply {
                            textAlign = Paint.Align.RIGHT
                        }
                        canvas.drawText(row.adminFeeStr, colX_Amount, textBaseY + 13.5f, feePaint)
                    }

                    yPos = rBottom
                }
            }

            // Draw Clean Footer (Ensuring spacious line height and no overlapping fonts)
            if (pageNum == totalPages) {
                // Ensure adequate margin from bottom of table
                yPos = maxOf(yPos + 22f, 745f)

                canvas.drawLine(38f, yPos, 557f, yPos, paintTableLine)
                yPos += 15f

                canvas.drawText(footerDisclaimer1, 38f, yPos, paintFooterText)
                yPos += 13f // Generous 13pt line height to completely prevent font collisions
                canvas.drawText(footerDisclaimer2, 38f, yPos, paintFooterText)
            }

            pdfDocument.finishPage(page)
        }

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun splitTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == "-") return listOf("-")

        val paragraphs = trimmed.split("\n")
        val allLines = mutableListOf<String>()

        for (paragraph in paragraphs) {
            val pTrim = paragraph.trim()
            if (pTrim.isEmpty()) continue

            if (paint.measureText(pTrim) <= maxWidth) {
                allLines.add(pTrim)
                continue
            }

            val words = pTrim.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        allLines.add(currentLine.toString())
                        currentLine = StringBuilder()
                    }
                    if (paint.measureText(word) > maxWidth) {
                        var subWord = ""
                        for (ch in word) {
                            if (paint.measureText(subWord + ch) <= maxWidth) {
                                subWord += ch
                            } else {
                                if (subWord.isNotEmpty()) allLines.add(subWord)
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
                allLines.add(currentLine.toString())
            }
        }

        return allLines.ifEmpty { listOf(trimmed) }
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
