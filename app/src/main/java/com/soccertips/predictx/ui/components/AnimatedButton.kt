package com.soccertips.predictx.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(),
    pulseEnabled: Boolean = false,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    minHeight: Dp = ButtonDefaults.MinHeight,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val coroutineScope = rememberCoroutineScope()

    //Scale animation logic can be added here if needed
    val pressScale = remember { Animatable(1f) }
    val hoverScale = remember { Animatable(1f) }
    val pulseScale = remember { Animatable(1f) }

    // Handle press and hover states
    LaunchedEffect(isPressed) {
        if (isPressed) {
            pressScale.animateTo(0.95f, tween(50))
        } else {
            pressScale.animateTo(1f, tween(100))
        }
    }
    LaunchedEffect(isHovered) {
        if (isHovered) {
            hoverScale.animateTo(1.05f, tween(100))
        } else {
            hoverScale.animateTo(1f, tween(100))
        }
    }

    if (pulseEnabled) {
        LaunchedEffect(pressScale) {
            pressScale.animateTo(
                initialVelocity = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = { it }),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Button(
        onClick = {
            coroutineScope.launch {
                pressScale.animateTo(0.95f, tween(50))
                pressScale.animateTo(1f, tween(100))
                onClick()
            }
        },
        modifier.graphicsLayer(
            scaleX = pressScale.value * hoverScale.value * pulseScale.value,
            scaleY = pressScale.value * hoverScale.value * pulseScale.value
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = elevation,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
    ) {
        Text(
            text = text,
            modifier
                .padding(horizontal = 8.dp)
                .sizeIn(minHeight = minHeight),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
    }

}
@Composable
fun ButtomExamples() {
    Column (modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)){
        //primary button example
        AnimatedButton(
            text = "Primary Button",
            onClick = { /* Handle click */ },

        )

        //secondary button example
        AnimatedButton(
            text = "Secondary Button",
            onClick = { /* Handle click */ },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp,
                hoveredElevation = 6.dp
            ),
            pulseEnabled = true
        )
        //Tertiary button example
        AnimatedButton(
            text = "Tertiary Button",
            onClick = { /* Handle click */ },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp,
                hoveredElevation = 6.dp
            ),
        )
    }
}

@Preview
@Composable
private fun ButtonExamplesPreview() {
    ButtomExamples()

}