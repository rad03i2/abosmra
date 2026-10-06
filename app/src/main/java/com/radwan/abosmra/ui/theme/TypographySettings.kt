package com.radwan.abosmra.ui.theme

import android.content.Context
import com.radwan.abosmra.R
import kotlin.math.roundToInt

enum class ArabicFontPreset(
    val storageValue: String,
    val title: String,
    val description: String,
    val receiptFontRes: Int
) {
    CAIRO(
        storageValue = "cairo",
        title = "Cairo",
        description = "حديث وواضح ومتوازن للاستخدام اليومي.",
        receiptFontRes = R.font.cairo_variable
    ),
    TAJAWAL(
        storageValue = "tajawal",
        title = "Tajawal",
        description = "أخف من Cairo ومريح للشاشات الصغيرة.",
        receiptFontRes = R.font.tajawal_regular
    ),
    NOTO_SANS_ARABIC(
        storageValue = "noto_sans_arabic",
        title = "Noto Sans Arabic",
        description = "واضح جدًا للنصوص والأرقام والواجهات.",
        receiptFontRes = R.font.noto_sans_arabic_variable
    ),
    NOTO_KUFI_ARABIC(
        storageValue = "noto_kufi_arabic",
        title = "Noto Kufi Arabic",
        description = "كوفي هندسي مميز للعناوين والواجهة.",
        receiptFontRes = R.font.noto_kufi_arabic_variable
    );

    companion object {
        fun fromStorage(value: String?): ArabicFontPreset =
            entries.firstOrNull { it.storageValue == value } ?: CAIRO
    }
}

data class TypographySettings(
    val font: ArabicFontPreset = ArabicFontPreset.CAIRO,
    val scale: Float = TypographySettingsStore.DEFAULT_SCALE
)

class TypographySettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun state(): TypographySettings =
        TypographySettings(
            font = ArabicFontPreset.fromStorage(
                prefs.getString(KEY_FONT, null)
            ),
            scale = sanitizeScale(
                prefs.getFloat(KEY_SCALE, DEFAULT_SCALE)
            )
        )

    fun setFont(font: ArabicFontPreset) {
        prefs.edit().putString(KEY_FONT, font.storageValue).apply()
    }

    fun setScale(scale: Float) {
        prefs.edit().putFloat(KEY_SCALE, sanitizeScale(scale)).apply()
    }

    fun reset() {
        prefs.edit()
            .putString(KEY_FONT, ArabicFontPreset.CAIRO.storageValue)
            .putFloat(KEY_SCALE, DEFAULT_SCALE)
            .apply()
    }

    companion object {
        const val MIN_SCALE = 0.85f
        const val MAX_SCALE = 1.10f
        const val DEFAULT_SCALE = 0.90f
        const val STEP = 0.05f

        private const val PREFS_NAME = "gas_ledger_typography_v1"
        private const val KEY_FONT = "font"
        private const val KEY_SCALE = "scale"

        fun sanitizeScale(value: Float): Float {
            val clamped = value.coerceIn(MIN_SCALE, MAX_SCALE)
            val steps = ((clamped - MIN_SCALE) / STEP).roundToInt()
            return (MIN_SCALE + steps * STEP)
                .coerceIn(MIN_SCALE, MAX_SCALE)
        }
    }
}
