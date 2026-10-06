package com.radwan.abosmra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TypographySettingsTest {

    @Test
    fun unknownStoredFont_fallsBackToCairo() {
        assertEquals(
            ArabicFontPreset.CAIRO,
            ArabicFontPreset.fromStorage("unknown-font")
        )
    }

    @Test
    fun storedFontValues_mapToExpectedFonts() {
        ArabicFontPreset.entries.forEach { preset ->
            assertEquals(
                preset,
                ArabicFontPreset.fromStorage(preset.storageValue)
            )
        }
    }

    @Test
    fun scale_isClampedToSupportedRange() {
        assertEquals(
            TypographySettingsStore.MIN_SCALE,
            TypographySettingsStore.sanitizeScale(0.1f),
            0.0001f
        )
        assertEquals(
            TypographySettingsStore.MAX_SCALE,
            TypographySettingsStore.sanitizeScale(5f),
            0.0001f
        )
    }

    @Test
    fun scale_snapsToFivePercentSteps() {
        assertEquals(
            0.90f,
            TypographySettingsStore.sanitizeScale(0.91f),
            0.0001f
        )
        assertEquals(
            1.05f,
            TypographySettingsStore.sanitizeScale(1.04f),
            0.0001f
        )
    }

    @Test
    fun defaultScale_isSmallerThanPreviousCairoSize() {
        assertEquals(
            0.90f,
            TypographySettingsStore.DEFAULT_SCALE,
            0.0001f
        )
    }
}
