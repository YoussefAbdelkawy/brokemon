package com.joecode.brokemon.share

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrBitmap {
    /** One pixel per QR module; scale it up in Compose with FilterQuality.None. */
    fun render(text: String, onColor: Int, offColor: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val bitmap = createBitmap(matrix.width, matrix.height)
        for (x in 0 until matrix.width) for (y in 0 until matrix.height) {
            bitmap[x, y] = if (matrix[x, y]) onColor else offColor
        }
        return bitmap
    }
}
