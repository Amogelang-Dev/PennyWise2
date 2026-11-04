package com.example.navigation

import android.util.Log
import java.util.regex.Pattern

/**
 * Utility class to parse financial data from notification text.
 */
object NotificationParser {
    /**
     * Extracts monetary values from unstructured strings.
     * Supports common South African formatting like "R 1,234.56" or "R123.45".
     */
    fun extractAmount(text: String): Float {
        // Look for 'R' followed by digits, spaces, commas, or dots, ending with a decimal part
        val pattern = Pattern.compile("R\\s?((?:\\d[\\d\\s,.]*)?[.,]\\d{2})")
        val matcher = pattern.matcher(text)

        return if (matcher.find()) {
            try {
                // Clean the string: remove whitespace and normalize commas to dots
                val cleaned = matcher.group(1)
                    ?.replace("\\s".toRegex(), "")
                    ?.replace(",", ".") ?: "0"

                // Handling cases with multiple separators (e.g., 1.000.00)
                val finalAmount = if (cleaned.count { it == '.' } > 1) {
                    val lastDotIndex = cleaned.lastIndexOf('.')
                    cleaned.substring(0, lastDotIndex).replace(".", "") + cleaned.substring(lastDotIndex)
                } else {
                    cleaned
                }

                finalAmount.toFloat()
            } catch (e: Exception) {
                0f
            }
        } else {
            0f
        }
    }
}
