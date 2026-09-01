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
        val index: Int,
        val dateStr: String,
        val timeStr: String,
        val categoryName: String,
        val walletInfo: String,
        val noteLines: List<String>,
        val adminFeeText: String?,
        val amountStr: String,
        val amountColor: Int,
        val balanceStr: String,
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
        val userName = viewModel.userGreetingName.value.ifBlank { "Sobat Duit" }

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
        val lastDayOfMonth = endCal.get(Calendar.DAY_OF_MONTH)

        // Filter transactions for this month
        val monthlyTx = transactions.filter { tx ->
            tx.date in startOfMonthMs..endOfMonthMs
        }.sortedBy { it.date }

        // 2. Calculate Exact Saldo Awal (Initial Balance)
        // Total Current Balance in all wallets right now:
        val currentTotalBalance = wallets.sumOf { it.balance }

        // Net change of all transactions that occurred from startOfMonthMs until now:
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

        // Saldo Awal at the start of requested month:
        val calculatedInitialBalance = (currentTotalBalance - netChangeSinceStart).coerceAtLeast(0.0)

        // Total Income & Expense for this month
        val totalIncome = monthlyTx.filter { it.type == "INCOME" }.sumOf { (it.amount - it.adminFee).coerceAtLeast(0.0) }
        val totalExpense = monthlyTx.filter { it.type == "EXPENSE" }.sumOf { it.amount + it.adminFee }
        val totalAdminFees = monthlyTx.sumOf { it.adminFee }
        val finalClosingBalance = calculatedInitialBalance + totalIncome - totalExpense - monthlyTx.filter { it.type == "TRANSFER" }.sumOf { it.adminFee }

        val monthName = getMonthName(month, isIndonesian)
        val periodDisplay = "01 $monthName $year - ${"%02d".format(lastDayOfMonth)} $monthName $year"

        // Localized Labels
        val stmtTitle = "e-Statement"
        val appBranding = "DUITKU FINANCIAL"
        val branchLabel = if (isIndonesian) "Akun / Dompet" else "Account / Wallet"
        val branchVal = if (wallets.size == 1) wallets.first().name else if (isIndonesian) "Semua Dompet (${wallets.size})" else "All Wallets (${wallets.size})"
        val nameLabel = if (isIndonesian) "Nama / Name" else "Name"
        val periodLabel = if (isIndonesian) "Periode / Period" else "Period"
        val printedLabel = if (isIndonesian) "Dicetak pada / Issued on" else "Issued on"
        val currencyLabel = if (isIndonesian) "Mata Uang / Currency" else "Currency"
        val pageLabel = if (isIndonesian) "Halaman" else "Page"
        val ofLabel = if (isIndonesian) "dari" else "of"

        val summaryTitle = if (isIndonesian) "Ringkasan Mutasi Rekening" else "Account Mutation Summary"
        val initialBalanceLbl = if (isIndonesian) "Saldo Awal / Initial Balance" else "Initial Balance"
        val incomingTxLbl = if (isIndonesian) "Dana Masuk / Incoming Transactions" else "Incoming Transactions"
        val outgoingTxLbl = if (isIndonesian) "Dana Keluar / Outgoing Transactions" else "Outgoing Transactions"
        val closingBalanceLbl = if (isIndonesian) "Saldo Akhir / Closing Balance" else "Closing Balance"

        val colNoLbl = "No"
        val colDateLbl = if (isIndonesian) "Tanggal\nDate" else "Date"
        val colDescLbl = if (isIndonesian) "Keterangan\nRemarks" else "Remarks"
        val colNominalLbl = if (isIndonesian) "Nominal (IDR)\nAmount (IDR)" else "Amount (IDR)"
        val colSaldoLbl = if (isIndonesian) "Saldo (IDR)\nBalance (IDR)" else "Balance (IDR)"

        // Paint setup
        val paintTopBanner = Paint().apply {
            color = Color.parseColor("#0284C7") // Primary Bank Cerulean Blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintTopBannerTitle = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTopBannerSub = Paint().apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintMetaKey = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintMetaVal = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintSummaryTitle = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSummaryKey = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintSummaryVal = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintIncomeVal = Paint().apply {
            color = Color.parseColor("#16A34A") // Bright Green
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintExpenseVal = Paint().apply {
            color = Color.parseColor("#DC2626") // Deep Red
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintClosingVal = Paint().apply {
            color = Color.parseColor("#0284C7") // Deep Blue
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTableHeaderBg = Paint().apply {
            color = Color.parseColor("#F0F9FF") // Light Blue Header tint
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintBorder = Paint().apply {
            color = Color.parseColor("#BAE6FD") // Light border
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
            isAntiAlias = true
        }

        val paintRowLine = Paint().apply {
            color = Color.parseColor("#E2E8F0") // Row divider line
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            isAntiAlias = true
        }

        val paintTableHeaderText = Paint().apply {
            color = Color.parseColor("#0369A1")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBodyRegular = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintBodyBold = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBodySub = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintAdminSub = Paint().apply {
            color = Color.parseColor("#D97706") // Amber warning/fee
            textSize = 6.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        // Table Column X Positions (Width = 595 - 60 = 535 pt, from 30f to 565f)
        val colX_No = 30f         // width 20f (30..50)
        val colX_Date = 50f       // width 65f (50..115)
        val colX_Desc = 115f      // width 230f (115..345)
        val colX_Amount = 455f    // right aligned at 455f (345..455)
        val colX_Balance = 565f   // right aligned at 565f (455..565)

        val widthDesc = 220f

        // Precompute Running Balances and Formatted Rows
        val sdfDate = SimpleDateFormat("dd MMM yyyy", if (isIndonesian) Locale("id", "ID") else Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm:ss", if (isIndonesian) Locale("id", "ID") else Locale.US)

        var currentRunningBalance = calculatedInitialBalance
        val formattedRows = mutableListOf<FormattedTxRow>()

        for ((idx, tx) in monthlyTx.withIndex()) {
            val walletName = wallets.firstOrNull { it.id == tx.walletId }?.name ?: "Dompet ${tx.walletId}"
            val targetWalletName = tx.targetWalletId?.let { tId -> wallets.firstOrNull { it.id == tId }?.name ?: "Dompet $tId" }
            val catName = if (tx.type == "TRANSFER") {
                if (isIndonesian) "Transfer Saldo Antar Dompet" else "Fund Transfer"
            } else {
                categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Lain-lain"
            }

            val dateStr = sdfDate.format(Date(tx.date))
            val timeStr = "${sdfTime.format(Date(tx.date))} WIB"

            val noteText = when {
                tx.type == "TRANSFER" && targetWalletName != null -> {
                    val base = if (isIndonesian) "Transfer: $walletName -> $targetWalletName" else "Transfer: $walletName -> $targetWalletName"
                    if (tx.note.isNotBlank()) "$base (${tx.note})" else base
                }
                tx.note.isNotBlank() -> tx.note
                else -> "-"
            }

            val noteLines = splitTextIntoLines(noteText, widthDesc, paintBodyRegular)

            val adminFeeText = if (tx.adminFee > 0.0) {
                if (isIndonesian) "Biaya Admin: +${viewModel.formatRupiah(tx.adminFee)}" else "Admin Fee: +${viewModel.formatRupiah(tx.adminFee)}"
            } else null

            // Amount calculation & Running Balance Adjustment
            val (amtStr, amtColor) = when (tx.type) {
                "INCOME" -> {
                    currentRunningBalance += (tx.amount - tx.adminFee).coerceAtLeast(0.0)
                    "+${viewModel.formatRupiah(tx.amount)}" to Color.parseColor("#16A34A")
                }
                "EXPENSE" -> {
                    currentRunningBalance -= (tx.amount + tx.adminFee)
                    "-${viewModel.formatRupiah(tx.amount)}" to Color.parseColor("#DC2626")
                }
                else -> {
                    currentRunningBalance -= tx.adminFee
                    "⇄ ${viewModel.formatRupiah(tx.amount)}" to Color.parseColor("#0284C7")
                }
            }

            val balanceStr = viewModel.formatRupiah(currentRunningBalance)

            // Calculate dynamic row height
            val descLinesCount = 1 + 1 + noteLines.size + (if (adminFeeText != null) 1 else 0) // Category + Wallet + Notes + AdminFee
            val rowHeight = maxOf(28f, 8f + (descLinesCount * 9.5f))

            formattedRows.add(
                FormattedTxRow(
                    index = idx + 1,
                    dateStr = dateStr,
                    timeStr = timeStr,
                    categoryName = catName,
                    walletInfo = if (tx.type == "TRANSFER") "Ref: Transaksi Dompet" else "Akun: $walletName",
                    noteLines = noteLines,
                    adminFeeText = adminFeeText,
                    amountStr = amtStr,
                    amountColor = amtColor,
                    balanceStr = balanceStr,
                    rowHeight = rowHeight
                )
            )
        }

        // 3. Multi-page Pagination Estimation
        // Page 1 header & summary card takes up to Y = 215f.
        // Table starts at Y = 220f. Table header is 24f. Max Y on Page 1 = 780f. Available space on Page 1 = 536f.
        // On Page 2+, top header is 45f, table starts at 55f (header 24f), available space = 700f.
        val pagesRows = mutableListOf<MutableList<FormattedTxRow>>()
        var curPageList = mutableListOf<FormattedTxRow>()
        var curPageY = 245f
        var isFirstPage = true

        if (formattedRows.isEmpty()) {
            pagesRows.add(mutableListOf())
        } else {
            for (row in formattedRows) {
                val maxY = if (isFirstPage) 780f else 800f
                if (curPageY + row.rowHeight > maxY && curPageList.isNotEmpty()) {
                    pagesRows.add(curPageList)
                    curPageList = mutableListOf()
                    isFirstPage = false
                    curPageY = 80f
                }
                curPageList.add(row)
                curPageY += row.rowHeight
            }
            if (curPageList.isNotEmpty()) {
                pagesRows.add(curPageList)
            }
        }

        val totalPages = pagesRows.size

        // 4. Render Pages
        val nowSdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", if (isIndonesian) Locale("id", "ID") else Locale.US)
        val printedTimeStr = nowSdf.format(Date())

        for ((pIdx, pageRows) in pagesRows.withIndex()) {
            val pageNum = pIdx + 1
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas = pdfPage.canvas

            var y = 0f

            if (pageNum == 1) {
                // Top Header Banner
                canvas.drawRect(0f, 0f, 595f, 48f, paintTopBanner)
                canvas.drawText(stmtTitle, 30f, 28f, paintTopBannerTitle)
                canvas.drawText(appBranding, 30f, 40f, paintTopBannerSub)

                val mLogoPaint = Paint(paintTopBannerTitle).apply {
                    textSize = 11f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("DUITKU MOBILE", 565f, 30f, mLogoPaint)

                // Customer & Period Info Grid
                y = 65f
                val colLeftX = 30f
                val colLeftValX = 90f
                val colRightX = 340f
                val colRightValX = 430f

                // Row 1
                canvas.drawText(nameLabel, colLeftX, y, paintMetaKey)
                canvas.drawText(": $userName", colLeftValX, y, paintMetaVal)

                canvas.drawText(periodLabel, colRightX, y, paintMetaKey)
                canvas.drawText(": $periodDisplay", colRightValX, y, paintMetaVal)

                val pageStr = "$pageNum $ofLabel $totalPages"
                val pRightPaint = Paint(paintMetaVal).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("$pageLabel: $pageStr", 565f, y, pRightPaint)

                // Row 2
                y += 13f
                canvas.drawText(branchLabel, colLeftX, y, paintMetaKey)
                canvas.drawText(": $branchVal", colLeftValX, y, paintMetaVal)

                canvas.drawText(printedLabel, colRightX, y, paintMetaKey)
                canvas.drawText(": $printedTimeStr", colRightValX, y, paintMetaVal)

                // Row 3
                y += 13f
                canvas.drawText(currencyLabel, colLeftX, y, paintMetaKey)
                canvas.drawText(": IDR (Rupiah)", colLeftValX, y, paintMetaVal)

                // Divider
                y += 10f
                canvas.drawLine(30f, y, 565f, y, paintRowLine)

                // Summary Box Section
                y += 14f
                canvas.drawText(summaryTitle, 30f, y, paintSummaryTitle)

                // Draw Summary Table Box
                y += 6f
                val sBoxTop = y
                val sBoxHeight = 44f
                val sBoxWidth = 535f
                val colSumWidth = sBoxWidth / 4f

                canvas.drawRect(30f, sBoxTop, 30f + sBoxWidth, sBoxTop + sBoxHeight, paintTableHeaderBg)
                canvas.drawRect(30f, sBoxTop, 30f + sBoxWidth, sBoxTop + sBoxHeight, paintBorder)

                // Draw 4 Summary Columns
                val sCols = listOf(
                    Triple(initialBalanceLbl, viewModel.formatRupiah(calculatedInitialBalance), paintSummaryVal),
                    Triple(incomingTxLbl, "+${viewModel.formatRupiah(totalIncome)}", paintIncomeVal),
                    Triple(outgoingTxLbl, "-${viewModel.formatRupiah(totalExpense)}", paintExpenseVal),
                    Triple(closingBalanceLbl, viewModel.formatRupiah(finalClosingBalance), paintClosingVal)
                )

                for ((cIdx, sCol) in sCols.withIndex()) {
                    val cx = 30f + (cIdx * colSumWidth)
                    if (cIdx > 0) {
                        canvas.drawLine(cx, sBoxTop, cx, sBoxTop + sBoxHeight, paintBorder)
                    }
                    canvas.drawText(sCol.first, cx + 8f, sBoxTop + 14f, paintSummaryKey)
                    canvas.drawText(sCol.second, cx + 8f, sBoxTop + 32f, sCol.third)
                }

                y = sBoxTop + sBoxHeight + 16f
            } else {
                // Subsequent page mini header
                canvas.drawRect(0f, 0f, 595f, 25f, paintTopBanner)
                canvas.drawText("$stmtTitle - $periodDisplay", 30f, 16f, Paint(paintTopBannerTitle).apply { textSize = 10f })

                val pageStr = "$pageLabel $pageNum $ofLabel $totalPages"
                val pRightPaint = Paint(paintTopBannerSub).apply { textAlign = Paint.Align.RIGHT; textSize = 9f }
                canvas.drawText(pageStr, 565f, 16f, pRightPaint)
                y = 40f
            }

            // Draw Table Header Row
            val headerH = 22f
            canvas.drawRect(30f, y, 565f, y + headerH, paintTableHeaderBg)
            canvas.drawRect(30f, y, 565f, y + headerH, paintBorder)

            canvas.drawText(colNoLbl, colX_No + 4f, y + 14f, paintTableHeaderText)
            canvas.drawText(colDateLbl.replace("\n", " / "), colX_Date + 4f, y + 14f, paintTableHeaderText)
            canvas.drawText(colDescLbl.replace("\n", " / "), colX_Desc + 4f, y + 14f, paintTableHeaderText)

            val rightHeaderPaint = Paint(paintTableHeaderText).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(colNominalLbl.replace("\n", " / "), colX_Amount - 6f, y + 14f, rightHeaderPaint)
            canvas.drawText(colSaldoLbl.replace("\n", " / "), colX_Balance - 6f, y + 14f, rightHeaderPaint)

            y += headerH

            // Draw Table Rows
            if (pageRows.isEmpty()) {
                canvas.drawRect(30f, y, 565f, y + 40f, paintBorder)
                val emptyPaint = Paint(paintBodySub).apply { textAlign = Paint.Align.CENTER; textSize = 8.5f }
                val msg = if (isIndonesian) "Tidak ada transaksi pada periode bulan ini." else "No transaction mutations recorded for this period."
                canvas.drawText(msg, 595f / 2f, y + 24f, emptyPaint)
                y += 40f
            } else {
                for (row in pageRows) {
                    val rTop = y
                    val rBottom = y + row.rowHeight

                    // Bottom line
                    canvas.drawLine(30f, rBottom, 565f, rBottom, paintRowLine)

                    // 1. No
                    canvas.drawText(row.index.toString(), colX_No + 4f, rTop + 13f, paintBodyRegular)

                    // 2. Date & Time
                    canvas.drawText(row.dateStr, colX_Date + 4f, rTop + 13f, paintBodyBold)
                    canvas.drawText(row.timeStr, colX_Date + 4f, rTop + 22f, paintBodySub)

                    // 3. Remarks (Category, Wallet, Note lines, Admin fee)
                    var descY = rTop + 13f
                    canvas.drawText(row.categoryName, colX_Desc + 4f, descY, paintBodyBold)
                    descY += 9.5f
                    canvas.drawText(row.walletInfo, colX_Desc + 4f, descY, paintBodySub)

                    for (nl in row.noteLines) {
                        descY += 9.5f
                        canvas.drawText(nl, colX_Desc + 4f, descY, paintBodyRegular)
                    }

                    if (row.adminFeeText != null) {
                        descY += 9.5f
                        canvas.drawText(row.adminFeeText, colX_Desc + 4f, descY, paintAdminSub)
                    }

                    // 4. Amount
                    val amtPaint = Paint(paintBodyBold).apply {
                        color = row.amountColor
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(row.amountStr, colX_Amount - 6f, rTop + 13f, amtPaint)

                    // 5. Saldo (Progressive Running Balance)
                    val balPaint = Paint(paintBodyBold).apply {
                        color = Color.parseColor("#0284C7")
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(row.balanceStr, colX_Balance - 6f, rTop + 13f, balPaint)

                    y = rBottom
                }
            }

            // Footer on last page
            if (pageNum == totalPages) {
                y += 18f
                if (y > 790f) {
                    // Small fallback offset
                    y = 790f
                }
                canvas.drawLine(30f, y, 565f, y, paintRowLine)
                y += 10f

                val paintFooter = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 6.8f
                    isAntiAlias = true
                }
                val disc1 = if (isIndonesian) {
                    "PT DuitKu Financial Application • Dokumen mutasi ini dibuat otomatis secara lokal dan sah sebagai catatan pembukuan pribadi."
                } else {
                    "DuitKu Financial Application • This mutation statement is automatically generated locally for personal accounting reference."
                }
                val disc2 = if (isIndonesian) {
                    "Informasi Saldo Awal, Dana Masuk, Dana Keluar, dan Saldo Akhir dihitung berdasarkan kalkulasi buku besar riwayat transaksi aplikasi."
                } else {
                    "Initial Balance, Incoming, Outgoing, and Closing Balances are calculated based on the application's transaction ledger."
                }
                canvas.drawText(disc1, 30f, y, paintFooter)
                canvas.drawText(disc2, 30f, y + 9f, paintFooter)
            }

            pdfDocument.finishPage(pdfPage)
        }

        // Write out PDF document
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
