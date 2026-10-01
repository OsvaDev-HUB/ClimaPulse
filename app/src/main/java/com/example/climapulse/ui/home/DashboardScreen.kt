package com.example.climapulse.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.climapulse.data.EnvironmentReading
import com.example.climapulse.data.IdealRange
import com.example.climapulse.data.Metric
import com.example.climapulse.ui.UiState
import com.example.climapulse.ui.components.ClimaCard
import com.example.climapulse.ui.components.ClimaLogo
import com.example.climapulse.ui.components.DashboardSkeleton
import com.example.climapulse.ui.components.ErrorState
import com.example.climapulse.ui.components.HeroCard
import com.example.climapulse.ui.components.IconBadge
import com.example.climapulse.ui.components.LinearMeter
import com.example.climapulse.ui.components.PrimaryButton
import com.example.climapulse.ui.components.SectionHeading
import com.example.climapulse.ui.components.StatusPill
import com.example.climapulse.ui.components.formatTime
import com.example.climapulse.ui.components.pressScale
import com.example.climapulse.ui.components.rememberInteractionSource
import com.example.climapulse.ui.theme.ClimaShapes
import com.example.climapulse.ui.theme.Dimens
import com.example.climapulse.ui.theme.MetricValueStyle
import com.example.climapulse.ui.theme.Teal
import com.example.climapulse.ui.theme.Yellow
import com.example.climapulse.ui.tour.TourKeys
import com.example.climapulse.ui.tour.tourTarget

@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onOpenMetric: (Metric) -> Unit,
    onOpenHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "dashboardState"
    ) { current ->
        when (current) {
            UiState.Loading -> DashboardSkeleton(PaddingValues())
            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize()
            )
            UiState.Empty -> Unit
            is UiState.Success -> DashboardContent(
                data = current.data,
                fromCache = current.fromCache,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                onSave = viewModel::saveMeasurement,
                onOpenMetric = onOpenMetric,
                onOpenHistory = onOpenHistory
            )
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    fromCache: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onOpenMetric: (Metric) -> Unit,
    onOpenHistory: () -> Unit
) {
    var trendMetric by rememberSaveable { mutableStateOf(Metric.TEMPERATURE) }
    val reading = data.reading
    fun range(metric: Metric) = data.ranges[metric] ?: metric.defaultRange

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        item { DashboardHeader(reading, fromCache, isRefreshing, onRefresh) }
        item {
            ComfortHero(
                reading = reading,
                ranges = data.ranges,
                onClick = { onOpenMetric(Metric.TEMPERATURE) },
                modifier = Modifier.tourTarget(TourKeys.HERO)
            )
        }
        item {
            Row(
                modifier = Modifier.tourTarget(TourKeys.METRICS),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
            ) {
                listOf(Metric.HUMIDITY, Metric.LIGHT).forEach { metric ->
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        metric = metric,
                        value = reading.valueOf(metric),
                        range = range(metric),
                        onClick = { onOpenMetric(metric) }
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = "Guardar medición actual",
                onClick = onSave,
                icon = Icons.Rounded.BookmarkAdd,
                modifier = Modifier.tourTarget(TourKeys.SAVE)
            )
        }
        item { SectionHeading(title = "Tendencia", action = "Ver historial", onAction = onOpenHistory) }
        item {
            TrendCard(trend = data.trend, selectedMetric = trendMetric, onMetricSelected = { trendMetric = it })
        }
        item { RecommendationCard(recommendationFor(reading, data.ranges)) }
    }
}

@Composable
private fun DashboardHeader(
    reading: EnvironmentReading,
    fromCache: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    // Microinteracción: el ícono gira mientras se actualiza.
    val rotation by rememberInfiniteTransition(label = "refresh").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "refreshRotation"
    )
    val subtitle = when {
        fromCache -> "Sin conexión · última lectura ${formatTime(reading.timestampMillis)}"
        reading.usingHardware -> "Sensor conectado · ${formatTime(reading.timestampMillis)}"
        else -> "Datos simulados · ${formatTime(reading.timestampMillis)}"
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ClimaLogo(size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("ClimaPulse", style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (fromCache) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalIconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            modifier = Modifier.tourTarget(TourKeys.REFRESH),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = if (isRefreshing) "Actualizando lecturas" else "Actualizar lecturas",
                modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier
            )
        }
    }
}

/** Tarjeta principal: estado general del ambiente + temperatura destacada. */
@Composable
private fun ComfortHero(
    reading: EnvironmentReading,
    ranges: Map<Metric, IdealRange>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val comfortable = isComfortable(reading, ranges)
    val metric = Metric.TEMPERATURE
    val range = ranges[metric] ?: metric.defaultRange
    val value = reading.temperature
    val statusColor = if (comfortable) Teal else Yellow
    val interactionSource = rememberInteractionSource()

    HeroCard(
        accent = statusColor,
        modifier = modifier
            .pressScale(interactionSource)
            .clip(ClimaShapes.ExtraLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = "Ver detalle de temperatura",
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = (if (comfortable) "Ambiente cómodo. " else "Revisa el ambiente. ") +
                    "Temperatura ${metric.formatWithUnit(value)}, ${metric.statusLabel(value, range)}"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                text = if (comfortable) "Ambiente cómodo" else "Revisa el ambiente",
                color = statusColor,
                icon = if (comfortable) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber
            )
            Spacer(Modifier.weight(1f))
            IconBadge(painter = metric.painter(), container = metric.accent.copy(alpha = 0.18f), size = 52.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text("Temperatura", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(metric.format(value), style = MetricValueStyle.copy(fontSize = MetricValueStyle.fontSize * 1.3f))
            Spacer(Modifier.width(6.dp))
            Text(
                metric.unit,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearMeter(progress = ((value - 10f) / 25f).coerceIn(0f, 1f), color = metric.accent)
        Spacer(Modifier.height(8.dp))
        Text(
            "Rango ideal ${metric.formatWithUnit(range.min)} – ${metric.formatWithUnit(range.max)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricCard(
    metric: Metric,
    value: Float,
    range: IdealRange,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = metric.statusLabel(value, range)
    val interactionSource = rememberInteractionSource()

    ClimaCard(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(ClimaShapes.Large)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = "Ver detalle de ${metric.label}",
                onClick = onClick
            )
            // Un solo anuncio para TalkBack en lugar de leer cada texto por separado.
            .semantics(mergeDescendants = true) {
                contentDescription = "${metric.label}: ${metric.formatWithUnit(value)}, $status"
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IconBadge(painter = metric.painter(), container = metric.accent.copy(alpha = 0.16f))
            Spacer(Modifier.height(16.dp))
            Text(metric.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(metric.format(value), style = MetricValueStyle.copy(fontSize = MetricValueStyle.fontSize * 0.7f))
                Spacer(Modifier.width(4.dp))
                Text(
                    metric.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            StatusPill(text = status, color = metric.accent)
        }
    }
}

@Composable
private fun TrendCard(
    trend: List<EnvironmentReading>,
    selectedMetric: Metric,
    onMetricSelected: (Metric) -> Unit
) {
    ClimaCard {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Metric.entries) { metric ->
                    FilterChip(
                        selected = metric == selectedMetric,
                        onClick = { onMetricSelected(metric) },
                        label = { Text(metric.label) },
                        leadingIcon = {
                            Icon(metric.painter(), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.width(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = metric.accent.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = ClimaShapes.Pill
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (trend.size < 2) {
                    // Estado vacío del gráfico: explica cómo llenarlo.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(ClimaShapes.Medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Guarda al menos dos mediciones para ver cómo cambia tu ambiente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val values = trend.map { it.valueOf(selectedMetric) }
                    Column {
                        TrendGraph(
                            values = values,
                            color = selectedMetric.accent,
                            description = "Tendencia de ${selectedMetric.label}: de ${selectedMetric.formatWithUnit(values.first())} a ${selectedMetric.formatWithUnit(values.last())}"
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            trend.forEach {
                                Text(
                                    formatTime(it.timestampMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendGraph(values: List<Float>, color: Color, description: String) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotFill = MaterialTheme.colorScheme.surfaceContainer
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = description }
    ) {
        val max = values.max()
        val min = values.min()
        val range = (max - min).coerceAtLeast(1f)
        val top = 10.dp.toPx()
        val usable = size.height - top * 2
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { index, value ->
            Offset(index * stepX, top + usable - (value - min) / range * usable)
        }
        repeat(3) { index ->
            val y = top + usable * index / 2f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val line = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    // Curva suave entre puntos.
                    val previous = points[index - 1]
                    val midX = (previous.x + point.x) / 2f
                    cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
                }
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { point ->
            drawCircle(color, radius = 5.dp.toPx(), center = point)
            drawCircle(dotFill, radius = 2.5.dp.toPx(), center = point)
        }
    }
}

@Composable
fun RecommendationCard(message: String) {
    HeroCard(accent = Yellow) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(icon = Icons.Rounded.Lightbulb, tint = MaterialTheme.colorScheme.secondary, size = 40.dp)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sugerencia", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
