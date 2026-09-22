package com.strategy.blackjacktrainer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strategy.blackjacktrainer.logic.Action
import com.strategy.blackjacktrainer.ui.theme.Emerald
import com.strategy.blackjacktrainer.ui.theme.Gold
import com.strategy.blackjacktrainer.ui.theme.OutlineSoft
import com.strategy.blackjacktrainer.ui.theme.SurfaceElevated
import com.strategy.blackjacktrainer.ui.theme.TextPrimary
import com.strategy.blackjacktrainer.ui.theme.TextSecondary

@Composable
fun ActionButtons(
    canDouble: Boolean,
    canSplit: Boolean,
    canSurrender: Boolean,
    enabled: Boolean,
    onChoose: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Hit", enabled, Modifier.weight(1f)) { onChoose(Action.HIT) }
            ActionButton("Stand", enabled, Modifier.weight(1f)) { onChoose(Action.STAND) }
            ActionButton("Double", enabled && canDouble, Modifier.weight(1f), accent = Gold) { onChoose(Action.DOUBLE) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Split", enabled && canSplit, Modifier.weight(1f)) { onChoose(Action.SPLIT) }
            ActionButton("Surrender", enabled && canSurrender, Modifier.weight(1f)) { onChoose(Action.SURRENDER) }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = Emerald,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceElevated,
            contentColor = TextPrimary,
            disabledContainerColor = SurfaceElevated.copy(alpha = 0.4f),
            disabledContentColor = TextSecondary.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) accent.copy(alpha = 0.6f) else OutlineSoft
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
