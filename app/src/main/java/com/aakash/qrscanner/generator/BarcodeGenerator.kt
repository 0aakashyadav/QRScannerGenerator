package com.aakash.qrscanner.generator

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.util.EnumMap

/**
 * Offline 1D barcode encoder for common retail and inventory symbologies.
 *
 * Validation is intentionally conservative: the library remains responsible for
 * final format-specific encoding, and invalid input is reported to the caller.
 */
object BarcodeGenerator {
    enum class Format(val label: String, internal val zxing: BarcodeFormat) {
        CODE_128("Code 128", BarcodeFormat.CODE_128),
        CODE_39("Code 39", BarcodeFormat.CODE_39),
        EAN_8("EAN-8", BarcodeFormat.EAN_8),
        EAN_13("EAN-13", BarcodeFormat.EAN_13),
        UPC_A("UPC-A", BarcodeFormat.UPC_A),
        ITF("ITF", BarcodeFormat.ITF),
        CODABAR("Codabar", BarcodeFormat.CODABAR)
    }

    data class Encoded(val bitmap: Bitmap, val normalizedValue: String, val format: Format)

    fun validate(value: String, format: Format): String? {
        val input = value.trim()
        if (input.isEmpty()) return "Enter a value to create a barcode."
        if (input.length > 256) return "Barcode content is too long."

        return when (format) {
            Format.EAN_8 -> {
                if (!input.matches(Regex("\\d{7,8}"))) "EAN-8 requires 7 digits (check digit generated) or 8 digits." else null
            }
            Format.EAN_13 -> {
                if (!input.matches(Regex("\\d{12,13}"))) "EAN-13 requires 12 digits (check digit generated) or 13 digits." else null
            }
            Format.UPC_A -> {
                if (!input.matches(Regex("\\d{11,12}"))) "UPC-A requires 11 digits (check digit generated) or 12 digits." else null
            }
            Format.ITF -> {
                if (!input.matches(Regex("\\d+"))) "ITF supports digits only."
                else if (input.length % 2 != 0) "ITF requires an even number of digits." else null
            }
            Format.CODE_39 -> {
                if (!input.matches(Regex("[0-9A-Z .$/+%_-]+"))) "Code 39 supports uppercase letters, digits and its standard punctuation." else null
            }
            Format.CODABAR -> {
                if (!input.matches(Regex("[0-9$:\\-/.+]+")) && !input.matches(Regex("[ABCD][0-9$:\\-/.+]*[ABCD]"))) {
                    "Codabar supports digits, selected punctuation, and optional A/B/C/D start-stop characters."
                } else null
            }
            Format.CODE_128 -> null
        }
    }

    /**
     * Encodes a barcode locally. The caller should catch IllegalArgumentException
     * and WriterException and show a readable validation message.
     */
    @Throws(WriterException::class)
    fun encode(
        value: String,
        format: Format,
        width: Int = 1200,
        height: Int = 420
    ): Encoded {
        require(width in 320..2400) { "Width must be between 320 and 2400 pixels." }
        require(height in 160..1200) { "Height must be between 160 and 1200 pixels." }
        val input = value.trim()
        validate(input, format)?.let { throw IllegalArgumentException(it) }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.MARGIN, 12)
        }
        val matrix: BitMatrix = MultiFormatWriter().encode(
            input,
            format.zxing,
            width,
            height,
            hints
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return Encoded(bitmap, input, format)
    }
}
