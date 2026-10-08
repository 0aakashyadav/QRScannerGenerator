package com.aakash.qrscanner.scanner

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

fun barcodeTitle(barcode: Barcode): String = when (barcode.valueType) {
    Barcode.TYPE_URL -> "Website"
    Barcode.TYPE_WIFI -> "Wi-Fi"
    Barcode.TYPE_CONTACT_INFO -> "Contact"
    Barcode.TYPE_EMAIL -> "Email"
    Barcode.TYPE_PHONE -> "Phone"
    Barcode.TYPE_SMS -> "SMS"
    Barcode.TYPE_GEO -> "Location"
    Barcode.TYPE_CALENDAR_EVENT -> "Calendar event"
    Barcode.TYPE_DRIVER_LICENSE -> "Driver license"
    else -> "Text"
}

fun isSafeWebUrl(value: String): Boolean = try {
    val uri = Uri.parse(value)
    (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrBlank()
} catch (_: Exception) { false }
