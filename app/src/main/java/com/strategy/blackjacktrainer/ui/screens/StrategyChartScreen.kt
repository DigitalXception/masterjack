package com.strategy.blackjacktrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strategy.blackjacktrainer.data.ScenarioStat
import com.strategy.blackjacktrainer.logic.Strategy
import com.strategy.blackjacktrainer.logic.TrainerViewModel
import com.strategy.blackjacktrainer.ui.theme.*

private val LabelWidth = 38.dp
private val HeaderHeight = 24.dp
@Composable
fun StrategyChartScreen(vm: TrainerViewModel) {
    LaunchedEffect(Unit) { vm.refreshScenarioStats() }

    val scenarioStats = vm.scenarioStats
    val sections = remember { Strategy.buildChartSections() }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text("Strategy Chart", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "The correct play for every hand vs. every dealer upcard. Color and % show your accuracy in that exact spot.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Legend()
        Spacer(Modifier.height(24.dp))

        sections.forEach { section ->
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ChartTable(section, scenarioStats)
            Spacer(Modifier.height(24.dp))
        }

        OutlinedButton(
            onClick = { showResetConfirm = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Chart Data")
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset chart data?") },
            text = { Text("This clears the accuracy coloring on the chart. Your mistake log and overall stats stay intact.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetScenarioStats()
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Legend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendSwatch(MistakeRed, "Struggling")
            LegendSwatch(Gold, "So-so")
            LegendSwatch(CorrectGreen, "Solid")
            LegendSwatch(NeutralCell, "No data yet")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "H Hit \u00b7 S Stand \u00b7 D Double \u00b7 P Split \u00b7 R Surrender",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun ChartTable(section: Strategy.ChartSection, stats: Map<String, ScenarioStat>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Fit all dealer columns to the available phone width. No sideways scrolling.
        val cellSize = ((maxWidth - LabelWidth) / Strategy.dealerColumns.size).coerceAtLeast(22.dp)

        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                HeaderCell("", cellSize)
                section.rows.forEach { row -> RowLabelCell(row.rowLabel, cellSize) }
            }
            Strategy.dealerColumns.forEachIndexed { colIndex, dealerLabel ->
                Column(modifier = Modifier.width(cellSize)) {
                    HeaderCell(dealerLabel, cellSize)
                    section.rows.forEach { row ->
                        val cell = row.cells[colIndex]
                        val key = "${cell.scenarioLabel}|${cell.dealerLabel}"
                        ChartCellView(cell, stats[key], cellSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, cellSize: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(cellSize)
            .height(HeaderHeight),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RowLabelCell(text: String, cellSize: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(LabelWidth)
            .height(cellSize)
            .padding(end = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun ChartCellView(cell: Strategy.ChartCell, stat: ScenarioStat?, cellSize: androidx.compose.ui.unit.Dp) {
    val hasData = stat != null && stat.attempts > 0
    val bg = if (hasData) accuracyColor(stat!!.accuracyPercent) else NeutralCell
    val textColor = if (hasData) OnPrimaryText else TextSecondary
    val action = Strategy.primaryActionFor(cell.code)

    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                action.shortCode,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                if (hasData) "${stat!!.accuracyPercent}" else "\u2013",
                color = textColor,
                fontSize = 7.sp
            )
        }
    }
}

/** Red (0%) through amber (50%) to green (100%). */
private fun accuracyColor(percent: Int): Color {
    val p = (percent.coerceIn(0, 100)) / 100f
    return if (p >= 0.5f) {
        lerp(Gold, CorrectGreen, (p - 0.5f) * 2f)
    } else {
        lerp(MistakeRed, Gold, p * 2f)
    }
}
