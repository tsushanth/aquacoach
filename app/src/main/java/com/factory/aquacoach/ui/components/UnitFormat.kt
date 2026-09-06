package com.factory.aquacoach.ui.components

import com.factory.aquacoach.data.preferences.MeasurementUnit
import java.util.Locale

object UnitFormat {
    private const val ML_PER_OZ = 29.5735f

    fun display(amountMl: Int, unit: MeasurementUnit): String = when (unit) {
        MeasurementUnit.ML -> "$amountMl ml"
        MeasurementUnit.OZ -> String.format(Locale.US, "%.1f oz", amountMl / ML_PER_OZ)
    }

    fun ozToMl(oz: Float): Int = (oz * ML_PER_OZ).let { Math.round(it) }
}
