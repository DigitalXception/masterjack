package com.strategy.blackjacktrainer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strategy.blackjacktrainer.data.PlayingCard
import com.strategy.blackjacktrainer.ui.theme.CardBack
import com.strategy.blackjacktrainer.ui.theme.CardFace
import com.strategy.blackjacktrainer.ui.theme.CardInk
import com.strategy.blackjacktrainer.ui.theme.CardRed
import com.strategy.blackjacktrainer.ui.theme.Gold

private val CardWidth = 76.dp
private val CardHeight = 104.dp

@Composable
fun PlayingCardView(card: PlayingCard, modifier: Modifier = Modifier) {
    val inkColor = if (card.suit.isRed) CardRed else CardInk

    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(CardFace)
            .padding(8.dp)
    ) {
        // Top-left rank + suit
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = card.rank,
                color = inkColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = card.suit.symbol,
                color = inkColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Center suit, large
        Text(
            text = card.suit.symbol,
            color = inkColor,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom-right rank + suit, upside-down feel via mirrored placement
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = card.suit.symbol,
                color = inkColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = card.rank,
                color = inkColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun FaceDownCardView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CardBack, CardBack.copy(alpha = 0.85f))
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Gold.copy(alpha = 0.12f))
        )
        Text(
            text = "\u2660",
            color = Gold.copy(alpha = 0.6f),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
