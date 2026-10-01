package com.example.climapulse.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.climapulse.R
import com.example.climapulse.data.EnvironmentReading
import com.example.climapulse.data.IdealRange
import com.example.climapulse.data.Metric
import com.example.climapulse.ui.components.formatNumber
import com.example.climapulse.ui.theme.Coral
import com.example.climapulse.ui.theme.Teal
import com.example.climapulse.ui.theme.Yellow

val Metric.accent: Color
    get() = when (this) {
        Metric.TEMPERATURE -> Coral
        Metric.HUMIDITY -> Teal
        Metric.LIGHT -> Yellow
    }

/** Ícono SVG duotono de la métrica (res/drawable/ic_metric_*.xml). */
val Metric.iconRes: Int
    @DrawableRes get() = when (this) {
        Metric.TEMPERATURE -> R.drawable.ic_metric_temperature
        Metric.HUMIDITY -> R.drawable.ic_metric_humidity
        Metric.LIGHT -> R.drawable.ic_metric_light
    }

@Composable
fun Metric.painter(): Painter = painterResource(iconRes)

val Metric.description: String
    get() = when (this) {
        Metric.TEMPERATURE -> "Temperatura del aire medida en grados Celsius. Entre 19 y 25 °C la mayoría de las personas se siente cómoda."
        Metric.HUMIDITY -> "Porcentaje de humedad relativa. Bajo 40 % el aire se siente seco; sobre 65 % favorece hongos y condensación."
        Metric.LIGHT -> "Iluminación en lux. Para leer o trabajar se recomiendan al menos 300 lux; las plantas de interior suelen necesitar más."
    }

fun Metric.format(value: Float): String = formatNumber(value, decimals)

fun Metric.formatWithUnit(value: Float): String = "${format(value)} $unit"

fun Metric.statusLabel(value: Float, range: IdealRange): String = when {
    value < range.min -> "Baja"
    value > range.max -> "Alta"
    else -> when (this) {
        Metric.TEMPERATURE -> "Ideal"
        Metric.HUMIDITY -> "Equilibrada"
        Metric.LIGHT -> "Adecuada"
    }
}

fun isComfortable(reading: EnvironmentReading, ranges: Map<Metric, IdealRange>): Boolean =
    Metric.entries.all { metric -> reading.valueOf(metric) in (ranges[metric] ?: metric.defaultRange) }

fun recommendationFor(reading: EnvironmentReading, ranges: Map<Metric, IdealRange>): String {
    val light = ranges[Metric.LIGHT] ?: Metric.LIGHT.defaultRange
    val humidity = ranges[Metric.HUMIDITY] ?: Metric.HUMIDITY.defaultRange
    val temperature = ranges[Metric.TEMPERATURE] ?: Metric.TEMPERATURE.defaultRange
    return when {
        reading.light < light.min -> "Hay poca luz. Acerca la planta o el espacio de trabajo a una ventana."
        reading.humidity > humidity.max -> "La humedad está alta. Ventila el espacio durante unos minutos."
        reading.humidity < humidity.min -> "El aire está seco. Un humidificador o plantas pueden ayudar."
        reading.temperature > temperature.max -> "La temperatura subió. Prueba con ventilación suave."
        reading.temperature < temperature.min -> "Hace frío. Cierra ventanas o enciende la calefacción."
        else -> "Todo se ve bien. Mantén la ventilación y la luz tal como están."
    }
}
