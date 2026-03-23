package com.haertibraeu.hopledger.ui.settings

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/** Encodes [url] and [apiKey] as a QR code bitmap. */
fun buildQrBitmap(url: String, apiKey: String, size: Int = 512): Bitmap? = runCatching {
    val payload = """{"url":${jsonString(url)},"apiKey":${jsonString(apiKey)}}"""
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i ->
        if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE
    }
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { it.setPixels(pixels, 0, size, 0, 0, size, size) }
}.getOrNull()

/** Parses a QR payload produced by [buildQrBitmap] and returns (url, apiKey), or null on failure. */
fun parseQrPayload(json: String): Pair<String, String>? = runCatching {
    val url = Regex(""""url"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(json)?.groupValues?.get(1) ?: return null
    val apiKey = Regex(""""apiKey"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(json)?.groupValues?.get(1) ?: ""
    Pair(unescapeJson(url), unescapeJson(apiKey))
}.getOrNull()

private fun jsonString(s: String) = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
private fun unescapeJson(s: String) = s.replace("\\\"", "\"").replace("\\\\", "\\")
