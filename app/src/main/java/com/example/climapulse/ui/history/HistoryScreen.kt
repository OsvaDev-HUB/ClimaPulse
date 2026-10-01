package com.example.climapulse.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.climapulse.data.EnvironmentReading
import com.example.climapulse.data.Metric
import com.example.climapulse.ui.UiState
import com.example.climapulse.ui.components.ClimaCard
import com.example.climapulse.ui.components.CollectMessages
import com.example.climapulse.ui.components.ConfirmDialog
import com.example.climapulse.ui.components.EmptyState
import com.example.climapulse.ui.components.ErrorState
import com.example.climapulse.ui.components.HeroCard
import com.example.climapulse.ui.components.IconBadge
import com.example.climapulse.ui.components.ListSkeleton
import com.example.climapulse.ui.components.ScreenTitle
import com.example.climapulse.ui.components.SectionHeading
import com.example.climapulse.ui.components.formatDate
import com.example.climapulse.ui.components.formatTime
import com.example.climapulse.ui.home.accent
import com.example.climapulse.ui.home.formatWithUnit
import com.example.climapulse.ui.home.painter
import com.example.climapulse.ui.theme.ClimaShapes
import com.example.climapulse.ui.theme.Coral
import com.example.climapulse.ui.theme.Dimens
import com.example.climapulse.ui.theme.MetricValueStyle

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    CollectMessages(viewModel.messages)

    if (confirmClear) {
        ConfirmDialog(
            title = "¿Borrar historial?",
            message = "Se eliminarán todas las mediciones guardadas. Esta acción no se puede deshacer.",
            confirmLabel = "Borrar",
            icon = Icons.Rounded.DeleteSweep,
            destructive = true,
            onConfirm = {
                confirmClear = false
                viewModel.clearHistory()
            },
            onDismiss = { confirmClear = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = Dimens.ScreenPadding, end = 8.dp, top = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                ScreenTitle(title = "Historial", subtitle = "Observa cómo cambia tu ambiente durante el día.")
            }
            if (state is UiState.Success) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Borrar historial")
                }
            }
        }
        // Los estados de carga, vacío y error van fuera de la lista: solo el éxito usa LazyColumn.
        when (val current = state) {
            UiState.Loading -> Box(Modifier.padding(Dimens.ScreenPadding)) { ListSkeleton() }
            is UiState.Error -> ErrorState(
                current.message,
                onRetry = viewModel::recordFirstMeasurement,
                modifier = Modifier.weight(1f)
            )
            UiState.Empty -> EmptyState(
                title = "Aún no hay mediciones",
                message = "Guarda tu primera lectura para empezar a ver tendencias y promedios del día.",
                actionLabel = "Registrar primera medición",
                onAction = viewModel::recordFirstMeasurement,
                loading = isSaving,
                modifier = Modifier.weight(1f)
            )
            is UiState.Success -> {
                val rows = current.data
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Dimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
                ) {
                    item { SummaryCard(rows) }
                    item { SectionHeading(title = "${rows.size} mediciones") }
                    items(rows, key = { it.timestampMillis }) { row -> HistoryRowItem(row) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(rows: List<EnvironmentReading>) {
    val metric = Metric.TEMPERATURE
    val temperatures = rows.map { it.temperature }
    val average = temperatures.average().toFloat()
    val change = rows.first().temperature - rows.last().temperature

    HeroCard(accent = Coral) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text("Temperatura promedio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(metric.formatWithUnit(average), style = MetricValueStyle.copy(fontSize = MetricValueStyle.fontSize * 0.8f))
            }
            IconBadge(painter = metric.painter(), container = metric.accent.copy(alpha = 0.18f), size = 52.dp)
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryStat("Mínima", metric.formatWithUnit(temperatures.min()), Icons.Rounded.ArrowDownward)
            SummaryStat("Máxima", metric.formatWithUnit(temperatures.max()), Icons.Rounded.ArrowUpward)
            SummaryStat("Variación", (if (change >= 0) "+" else "") + metric.formatWithUnit(change), Icons.AutoMirrored.Rounded.TrendingUp)
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(ClimaShapes.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HistoryRowItem(row: EnvironmentReading) {
    ClimaCard(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Rounded.Schedule, tint = MaterialTheme.colorScheme.primary, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatTime(row.timestampMillis), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(row.timestampMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // FlowRow: con texto grande del sistema los valores bajan de línea en vez de cortarse.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Metric.entries.forEach { metric -> ReadingPill(metric, metric.formatWithUnit(row.valueOf(metric))) }
                }
            }
        }
    }
}

@Composable
private fun ReadingPill(metric: Metric, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(metric.painter(), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
