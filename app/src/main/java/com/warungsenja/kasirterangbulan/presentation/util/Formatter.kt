package com.warungsenja.kasirterangbulan.presentation.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatter {
    private val indonesiaLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long): String {
        val numberFormat = NumberFormat.getNumberInstance(indonesiaLocale)
        return "Rp ${numberFormat.format(amount)}"
    }

    fun formatDateTime(isoString: String): String {
        if (isoString.isBlank()) return "-"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanIso = isoString.take(19)
            val parsedDate = parser.parse(cleanIso) ?: Date()
            val outFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale)
            outFormat.format(parsedDate)
        } catch (_: Exception) {
            isoString.take(16).replace("T", " ")
        }
    }

    fun nowIsoString(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date())
    }

    fun generateTransactionCode(type: String, currentCount: Int): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val seq = "%03d".format(currentCount + 1)
        val prefix = if (type.equals("IN", ignoreCase = true)) "IN" else "TB"
        return "$prefix-$dateStr-$seq"
    }

    fun generateMenuBarcode(index: Int): String {
        return "%02d".format(index.coerceAtLeast(1))
    }
}
