package com.strategy.blackjacktrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strategy.blackjacktrainer.data.MistakeEntry
import com.strategy.blackjacktrainer.logic.TrainerViewModel
import com.strategy.blackjacktrainer.ui.theme.*

@Composable
fun MistakesScreen(vm: TrainerViewModel) {
    LaunchedEffect(Unit) { vm.refreshMistakes() }

    var showConfirm by remember { mutableStateOf(false) }
    val mistakes = vm.mistakes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Common Mistakes", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Scenarios you've missed, ranked by how often \u2014 practice these first.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        if (mistakes.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(mistakes, key = { it.key }) { entry ->
                    MistakeRow(entry)
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear History")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Clear mistake history?") },
            text = { Text("This removes all logged mistakes. Your overall accuracy stats stay intact.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearMistakes()
                    showConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("\u2660 \u2665", style = MaterialTheme.typography.headlineMedium, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        Text(
            "No mistakes logged yet.",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Keep training \u2014 anything you miss will show up here, sorted by frequency.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun MistakeRow(entry: MistakeEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MistakeRed.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${entry.count}\u00d7", color = MistakeRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${entry.handLabel} vs dealer ${entry.dealerLabel}",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "You tend to ${entry.lastChosenAction} \u2014 correct play is ${entry.correctAction}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
