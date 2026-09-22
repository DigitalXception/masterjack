package com.strategy.blackjacktrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strategy.blackjacktrainer.logic.DiagnosticPhase
import com.strategy.blackjacktrainer.logic.Feedback
import com.strategy.blackjacktrainer.logic.PlayerHand
import com.strategy.blackjacktrainer.logic.Strategy
import com.strategy.blackjacktrainer.logic.TrainerViewModel
import com.strategy.blackjacktrainer.logic.TrainingMode
import com.strategy.blackjacktrainer.ui.components.ActionButtons
import com.strategy.blackjacktrainer.ui.components.FaceDownCardView
import com.strategy.blackjacktrainer.ui.components.PlayingCardView
import com.strategy.blackjacktrainer.ui.theme.*

@Composable
fun TrainerScreen(vm: TrainerViewModel, onPopOut: () -> Unit) {
    var showSettings by remember { mutableStateOf(false) }
    val hands = vm.hands
    val feedback = vm.feedback
    val stats = vm.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                StatsRow(accuracy = stats.accuracyPercent, hands = vm.handsPlayed, streak = vm.streak)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Emerald
                )
            }
            IconButton(onClick = onPopOut) {
                Icon(
                    Icons.Filled.PictureInPictureAlt,
                    contentDescription = "Pop out trainer",
                    tint = Emerald
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ModeSelector(vm)

        if (vm.trainingMode == TrainingMode.DIAGNOSTIC) {
            Spacer(Modifier.height(12.dp))
            DiagnosticStatusCard(vm)
        }

        Spacer(Modifier.height(20.dp))

        Text("DEALER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayingCardView(vm.dealer)
            FaceDownCardView()
        }

        Spacer(Modifier.height(28.dp))

        hands.forEachIndexed { index, hand ->
            val isActive = index == vm.activeHandIndex
            HandBlock(
                hand = hand,
                title = if (vm.isMultiHand) "HAND ${index + 1} OF ${hands.size}" else "YOUR HAND",
                isActive = isActive
            )
            if (index != hands.lastIndex) Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(28.dp))

        if (feedback?.wasCorrect == true) {
            FeedbackCard(feedback)
            Spacer(Modifier.height(12.dp))
        }

        ActionButtons(
            canDouble = vm.canDoubleNow,
            canSplit = vm.canSplitNow,
            canSurrender = vm.canSurrenderNow,
            enabled = feedback?.wasCorrect != false && vm.diagnosticPhase.let { it != DiagnosticPhase.COMPLETE && it != DiagnosticPhase.TRANSITION },
            onChoose = { vm.choose(it) },
            modifier = Modifier.fillMaxWidth()
        )

        if (vm.trainingMode == TrainingMode.DIAGNOSTIC) {
            Spacer(Modifier.height(18.dp))
            DiagnosticScoreCard(vm)
        }

        Spacer(Modifier.height(18.dp))
        DecisionHistoryCard(vm.decisionHistory)

        Spacer(Modifier.height(24.dp))
    }


    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }

    val incorrect = feedback?.takeIf { !it.wasCorrect }
    if (incorrect != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Incorrect decision") },
            text = {
                Column {
                    Text(
                        "You chose ${incorrect.chosen.label}. Correct play: ${incorrect.correct.label}.",
                        color = TextPrimary
                    )
                    incorrect.note?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.continueRound() }) {
                    Text("Continue")
                }
            }
        )
    }

    if (vm.diagnosticPhase == DiagnosticPhase.TRANSITION) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Diagnosis complete") },
            text = {
                Text(
                    "First pass score: ${vm.diagnosticScoreText}. Now beginning error correction with ${vm.diagnosticRemainingCorrections} missed cases."
                )
            },
            confirmButton = {
                TextButton(onClick = vm::advanceDiagnosticTransition) {
                    Text("Begin Error Correction")
                }
            }
        )
    }

    if (vm.diagnosticPhase == DiagnosticPhase.COMPLETE && vm.trainingMode == TrainingMode.DIAGNOSTIC) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Diagnostic complete") },
            text = {
                Text(
                    "Score: ${vm.diagnosticScoreText}. Every case you missed in the first pass was revisited until you got it right."
                )
            },
            confirmButton = {
                TextButton(onClick = vm::restartDiagnostic) {
                    Text("Run Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.changeTrainingMode(TrainingMode.RANDOM) }) {
                    Text("Return to Random")
                }
            }
        )
    }
}

@Composable
private fun ModeSelector(vm: TrainerViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { vm.changeTrainingMode(TrainingMode.RANDOM) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (vm.trainingMode == TrainingMode.RANDOM) Emerald else SurfaceDark,
                contentColor = if (vm.trainingMode == TrainingMode.RANDOM) TableBg else TextPrimary
            )
        ) { Text("Random") }
        Button(
            onClick = { vm.changeTrainingMode(TrainingMode.DIAGNOSTIC) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (vm.trainingMode == TrainingMode.DIAGNOSTIC) Emerald else SurfaceDark,
                contentColor = if (vm.trainingMode == TrainingMode.DIAGNOSTIC) TableBg else TextPrimary
            )
        ) { Text("Test Mode") }
    }
}

@Composable
private fun DiagnosticStatusCard(vm: TrainerViewModel) {
    val phaseText = when (vm.diagnosticPhase) {
        DiagnosticPhase.FIRST_PASS -> "Diagnosis"
        DiagnosticPhase.TRANSITION -> "Diagnosis complete"
        DiagnosticPhase.CORRECTION -> "Error correction"
        DiagnosticPhase.COMPLETE -> "Completed"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(phaseText, color = Emerald, fontWeight = FontWeight.Bold)
            Text("Score ${vm.diagnosticScoreText}", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        val detail = when (vm.diagnosticPhase) {
            DiagnosticPhase.FIRST_PASS -> "Starting case ${vm.diagnosticCurrentNumber} of ${vm.diagnosticTotal} • ${vm.diagnosticDecisionCount} decisions logged"
            DiagnosticPhase.CORRECTION -> "${vm.diagnosticRemainingCorrections} missed decision cases remain — hardest misses are prioritized"
            DiagnosticPhase.TRANSITION -> "First pass finished"
            DiagnosticPhase.COMPLETE -> "All missed cases mastered"
        }
        Text(detail, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DiagnosticScoreCard(vm: TrainerViewModel) {
    if (vm.trainingMode != TrainingMode.DIAGNOSTIC) return
    Text(
        "First-pass decision score: ${vm.diagnosticScoreText}  •  Starting cases: ${vm.diagnosticTotal}  •  Correction remaining: ${vm.diagnosticRemainingCorrections}",
        color = TextSecondary,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TrainerPipScreen(vm: TrainerViewModel) {
    val feedback = vm.feedback
    val legal = buildList {
        add(com.strategy.blackjacktrainer.logic.Action.HIT)
        add(com.strategy.blackjacktrainer.logic.Action.STAND)
        if (vm.canDoubleNow) add(com.strategy.blackjacktrainer.logic.Action.DOUBLE)
        if (vm.canSplitNow) add(com.strategy.blackjacktrainer.logic.Action.SPLIT)
        if (vm.canSurrenderNow) add(com.strategy.blackjacktrainer.logic.Action.SURRENDER)
    }
    val buttonsEnabled = feedback?.wasCorrect != false &&
        vm.diagnosticPhase != DiagnosticPhase.COMPLETE &&
        vm.diagnosticPhase != DiagnosticPhase.TRANSITION

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TableBg)
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PipValueCard("DEALER", vm.dealer.pointValue, Modifier.weight(1f))
                PipValueCard("PLAYER", vm.activeHand.total, Modifier.weight(1f))
            }

            Text(
                text = if (vm.trainingMode == TrainingMode.DIAGNOSTIC)
                    "TEST ${vm.diagnosticCurrentNumber}/${vm.diagnosticTotal}"
                else "CHOOSE ACTION",
                color = Emerald,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                legal.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        row.forEach { action ->
                            PipActionButton(
                                label = action.label,
                                enabled = buttonsEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = { vm.choose(action) }
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (feedback?.wasCorrect == false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = SurfaceDark,
                tonalElevation = 12.dp,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "INCORRECT",
                        color = MistakeRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Correct: ${feedback.correct.label}",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PipActionButton(
                        label = "CONTINUE",
                        enabled = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { vm.continueRound() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PipValueCard(label: String, total: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 42.dp),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = TextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(total.toString(), color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PipActionButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) SurfaceDark else SurfaceDark.copy(alpha = 0.45f),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun HandBlock(hand: PlayerHand, title: String, isActive: Boolean) {
    val read = Strategy.readHand(hand.cards)
    val label = if (hand.finished && !isActive) "${read.label} — ${hand.resultLabel}" else read.label

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .padding(12.dp)
                else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$title — $label",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Emerald else TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            hand.cards.forEach { PlayingCardView(it) }
        }
    }
}

@Composable
private fun DecisionHistoryCard(history: List<com.strategy.blackjacktrainer.logic.DecisionHistoryEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(14.dp)
    ) {
        Text(
            "Recent decisions",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text("Your played hands will appear here.", color = TextSecondary)
        } else {
            val recent = history.asReversed().take(10)
            recent.forEachIndexed { index, entry ->
                val mark = if (entry.wasCorrect) "✓" else "✕"
                val markColor = if (entry.wasCorrect) CorrectGreen else MistakeRed
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${entry.playerLabel} vs ${entry.dealerLabel}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(mark, color = markColor, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${entry.chosen.label}  •  Correct: ${entry.correct.label}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (index != recent.lastIndex) HorizontalDivider(color = OutlineSoft.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun StatsRow(accuracy: Int, hands: Int, streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Accuracy", "$accuracy%")
        StatItem("Hands", "$hands")
        StatItem("Streak", "$streak")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun FeedbackCard(feedback: Feedback) {
    val bg = if (feedback.wasCorrect) CorrectGreen.copy(alpha = 0.14f) else MistakeRed.copy(alpha = 0.14f)
    val accent = if (feedback.wasCorrect) CorrectGreen else MistakeRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(16.dp)
    ) {
        Text(
            text = if (feedback.wasCorrect) "✓ Correct" else "✗ Not quite",
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (feedback.wasCorrect) "${feedback.correct.label} is right." else "You chose ${feedback.chosen.label}. Correct play: ${feedback.correct.label}.",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
        feedback.note?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var blackAndWhite by remember { mutableStateOf(AppColorMode.blackAndWhite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Black & white mode", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("Use a grayscale interface across the trainer.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = blackAndWhite,
                        onCheckedChange = {
                            blackAndWhite = it
                            AppColorMode.blackAndWhite = it
                            context.getSharedPreferences("trainer_settings", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("black_and_white", it)
                                .apply()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
