package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.R

data class CategoryVisualInfo(
    val iconRes: Int,
    val backgroundColor: Color,
    val iconColor: Color
)

object CategoryVisuals {

    fun getVisualInfo(
        categoryName: String?,
        transactionType: String,
        note: String = ""
    ): CategoryVisualInfo {
        if (transactionType == "TRANSFER") {
            return CategoryVisualInfo(
                iconRes = R.drawable.ic_cat_transfer,
                backgroundColor = Color(0xFFE3F2FD),
                iconColor = Color(0xFF1565C0)
            )
        }

        val nameLower = (categoryName ?: "").lowercase().trim()
        val noteLower = note.lowercase().trim()
        val combined = "$nameLower $noteLower"

        return when {
            // Food & Drinks
            combined.contains("makan") || combined.contains("minum") || combined.contains("food") ||
                    combined.contains("drink") || combined.contains("resto") || combined.contains("kuliner") ||
                    combined.contains("cafe") || combined.contains("kopi") || combined.contains("snack") ||
                    combined.contains("lunch") || combined.contains("dinner") || combined.contains("breakfast") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_food,
                    backgroundColor = Color(0xFFFFECE6),
                    iconColor = Color(0xFFE65100)
                )
            }

            // Transport & Fuel
            combined.contains("transpor") || combined.contains("bensin") || combined.contains("ojek") ||
                    combined.contains("grab") || combined.contains("gojek") || combined.contains("parkir") ||
                    combined.contains("car") || combined.contains("motor") || combined.contains("kendaraan") ||
                    combined.contains("tol") || combined.contains("fuel") || combined.contains("commute") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_transport,
                    backgroundColor = Color(0xFFE0F7FA),
                    iconColor = Color(0xFF00838F)
                )
            }

            // Shopping & Groceries
            combined.contains("belanja") || combined.contains("shop") || combined.contains("mart") ||
                    combined.contains("market") || combined.contains("mall") || combined.contains("indomaret") ||
                    combined.contains("alfamart") || combined.contains("supermarket") || combined.contains("pasar") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_shopping,
                    backgroundColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF7B1FA2)
                )
            }

            // Bills & Utilities & Installments
            combined.contains("tagihan") || combined.contains("bill") || combined.contains("listrik") ||
                    combined.contains("air") || combined.contains("pdam") || combined.contains("wifi") ||
                    combined.contains("internet") || combined.contains("pulsa") || combined.contains("paket data") ||
                    combined.contains("pln") || combined.contains("utilit") || combined.contains("cicilan") ||
                    combined.contains("spaylater") || combined.contains("gopaylater") || combined.contains("paylater") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_bills,
                    backgroundColor = Color(0xFFFFF8E1),
                    iconColor = Color(0xFFF57F17)
                )
            }

            // Entertainment & Hobbies
            combined.contains("hiburan") || combined.contains("entertain") || combined.contains("game") ||
                    combined.contains("nonton") || combined.contains("bioskop") || combined.contains("movie") ||
                    combined.contains("netflix") || combined.contains("spotify") || combined.contains("steam") ||
                    combined.contains("liburan") || combined.contains("hobby") || combined.contains("hobi") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_entertainment,
                    backgroundColor = Color(0xFFFCE4EC),
                    iconColor = Color(0xFFC2185B)
                )
            }

            // Salary & Wages
            combined.contains("gaji") || combined.contains("salary") || combined.contains("wage") ||
                    combined.contains("payroll") || combined.contains("upah") || combined.contains("honor") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_salary,
                    backgroundColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32)
                )
            }

            // Bonus & Rewards
            combined.contains("bonus") || combined.contains("thr") || combined.contains("hadiah") ||
                    combined.contains("gift") || combined.contains("reward") || combined.contains("insentif") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_bonus,
                    backgroundColor = Color(0xFFFFFDE7),
                    iconColor = Color(0xFFF9A825)
                )
            }

            // Investment & Profits
            combined.contains("invest") || combined.contains("saham") || combined.contains("reksa") ||
                    combined.contains("crypto") || combined.contains("tabungan") || combined.contains("profit") ||
                    combined.contains("dividen") || combined.contains("deposito") || combined.contains("emas") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_investment,
                    backgroundColor = Color(0xFFE0F2F1),
                    iconColor = Color(0xFF00695C)
                )
            }

            // Refunds & Claims & Cashbacks
            combined.contains("refund") || combined.contains("klaim") || combined.contains("kembali") ||
                    combined.contains("cashback") || combined.contains("reimburse") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_refund,
                    backgroundColor = Color(0xFFE1F5FE),
                    iconColor = Color(0xFF0277BD)
                )
            }

            // Health & Medical
            combined.contains("sehat") || combined.contains("obat") || combined.contains("dokter") ||
                    combined.contains("rumah sakit") || combined.contains("apotek") || combined.contains("health") ||
                    combined.contains("medis") || combined.contains("vitamin") || combined.contains("klinik") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_health,
                    backgroundColor = Color(0xFFFFEBEE),
                    iconColor = Color(0xFFD32F2F)
                )
            }

            // Education & Books
            combined.contains("didik") || combined.contains("sekolah") || combined.contains("kuliah") ||
                    combined.contains("kursus") || combined.contains("buku") || combined.contains("spp") ||
                    combined.contains("educat") || combined.contains("course") || combined.contains("training") -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_education,
                    backgroundColor = Color(0xFFE8EAF6),
                    iconColor = Color(0xFF283593)
                )
            }

            // General Income
            transactionType == "INCOME" -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_salary,
                    backgroundColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32)
                )
            }

            // General Expense / Other
            else -> {
                CategoryVisualInfo(
                    iconRes = R.drawable.ic_cat_other,
                    backgroundColor = Color(0xFFFFEBEE),
                    iconColor = Color(0xFFC62828)
                )
            }
        }
    }
}
