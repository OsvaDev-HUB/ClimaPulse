package com.example.climapulse.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.climapulse.data.Metric
import com.example.climapulse.ui.components.ClimaCard
import com.example.climapulse.ui.components.HeroCard
import com.example.climapulse.ui.components.IconBadge
import com.example.climapulse.ui.components.LocalMessenger
import com.example.climapulse.ui.components.PrimaryButton
import com.example.climapulse.ui.components.PulseRings
import com.example.climapulse.ui.components.ScreenTitle
import com.example.climapulse.ui.components.SectionHeading
import com.example.climapulse.ui.components.StatusPill
import com.example.climapulse.ui.theme.Dimens
import com.example.climapulse.ui.theme.Teal
import com.example.climapulse.ui.theme.Yellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SensorScreen(viewModel: HomeViewModel) {
    val usingHardware = viewModel.hasHardwareSensors
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        ScreenTitle(title = "Sensor", subtitle = "Administra la fuente de datos de tu ambiente.")
        Spacer(Modifier.height(4.dp))

        HeroCard(accent = if (usingHardware) Teal else Yellow) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // El pulso se acelera visualmente mientras busca: mismo lenguaje del logo.
                PulseRings(size = 170.dp, color = if (isSearching || usingHardware) Teal else Yellow) {
                    IconBadge(
                        icon = Icons.Rounded.Sensors,
                        tint = if (usingHardware) Teal else Yellow,
                        size = 72.dp
                    )
                }
                StatusPill(
                    text = if (usingHardware) "Sensor conectado" else "Modo demostración",
                    color = if (usingHardware) Teal else Yellow
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (usingHardware) "Lecturas recibidas desde el dispositivo." else "La app está usando valores simulados mientras conectas un sensor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = if (isSearching) "Buscando sensor..." else "Buscar sensor",
                    icon = Icons.Rounded.Search,
                    loading = isSearching,
                    onClick = {
                        isSearching = true
                        viewModel.refresh(successMessage = "Lecturas sincronizadas")
                        scope.launch {
                            delay(1_500)
                            isSearching = false
                            if (!usingHardware) {
                                messenger.show("No encontramos un sensor externo. Seguimos con datos de demostración.")
                            }
                        }
                    }
                )
            }
        }

        SectionHeading(title = "Lecturas que puede recibir")
        ClimaCard {
            Metric.entries.forEachIndexed { index, metric ->
                ListItem(
                    headlineContent = { Text(metric.label) },
                    supportingContent = {
                        Text(
                            when (metric) {
                                Metric.TEMPERATURE -> "Medición en grados Celsius"
                                Metric.HUMIDITY -> "Humedad relativa del ambiente"
                                Metric.LIGHT -> "Nivel de iluminación en lux"
                            }
                        )
                    },
                    leadingContent = { IconBadge(painter = metric.painter(), container = metric.accent.copy(alpha = 0.16f)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (index < Metric.entries.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        ClimaCard(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            ListItem(
                headlineContent = { Text("Listo para hardware externo") },
                supportingContent = {
                    Text("La conexión queda preparada para un módulo Bluetooth o ESP32 cuando definamos su protocolo.")
                },
                leadingContent = { Icon(Icons.Rounded.Bluetooth, contentDescription = null) },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    supportingColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    leadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
