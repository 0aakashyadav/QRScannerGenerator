package com.aakash.qrscanner.generator

import android.net.Uri

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrGenerator {
    fun encode(content: String, size: Int = 1024): Bitmap {
        require(content.isNotBlank())
        require(size in 256..2048)
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.MARGIN, 2)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        return bitmap
    }

    fun wifi(ssid: String, password: String, security: String, hidden: Boolean): String =
        "WIFI:T:${escape(security)};S:${escape(ssid)};P:${escape(password)};H:${if (hidden) "true" else "false"};;"

    fun vCard(name: String, phone: String, email: String, organization: String): String = buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        append("FN:${escapeVCard(name)}\n")
        if (phone.isNotBlank()) append("TEL:${escapeVCard(phone)}\n")
        if (email.isNotBlank()) append("EMAIL:${escapeVCard(email)}\n")
        if (organization.isNotBlank()) append("ORG:${escapeVCard(organization)}\n")
        append("END:VCARD")
    }

    fun location(lat: String, lon: String, label: String): String =
        "geo:${lat.trim()},${lon.trim()}?q=${Uri.encode(label.trim().ifBlank { "Location" })}"

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace(":", "\\:")
        .replace("\n", "\\n")
        .replace("\r", "")

    private fun escapeVCard(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
        .replace("\r", "")
}


