package com.soccertips.predictx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predictx.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



@Composable
fun SplashScreen(
    navController: NavController,
    initialFixtureId: String?,
    onSplashCompleted: () -> Unit
) {
    // Animation state management
    var animationState by remember { mutableStateOf(SplashAnimationState.Initial) }
    val density = LocalDensity.current

    // Enhanced theming for better visual appeal
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.background
    }

    // More dynamic gradient background
    val gradientColors = if (isDarkTheme) {
        listOf(
            backgroundColor,
            primaryColor.copy(alpha = 0.2f),
            backgroundColor
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            backgroundColor
        )
    }

    // Single source of truth for animation
    val logoAnimatable = remember { Animatable(0f) }

    // Create a subtle pulsing effect once logo appears
    val pulseFactor by rememberInfiniteTransition(label = "logoPulse").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnimation"
    )

    // Derive scale and alpha from animation progress
    val logoScale = if (logoAnimatable.value < 0.5f) {
        0.8f + (logoAnimatable.value * 0.4f)
    } else {
        1f * if (animationState >= SplashAnimationState.ShowTagline) pulseFactor else 1f
    }

    val logoAlpha = (logoAnimatable.value * 2f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors))
            .semantics { contentDescription = "Splash Screen" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Optimized logo animation
            Image(
                painter = painterResource(id = R.drawable.launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(
                        alpha = logoAlpha,
                        scaleX = logoScale,
                        scaleY = logoScale
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tagline with slide-in animation
            AnimatedVisibility(
                visible = animationState >= SplashAnimationState.ShowTagline,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { with(density) { 20.dp.roundToPx() } },
                    animationSpec = tween(500)
                )
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Loading indicator with fade-in
            AnimatedVisibility(
                visible = animationState >= SplashAnimationState.ShowLoading,
                enter = fadeIn(tween(400))
            ) {
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }

    // Optimized animation sequence with better timing
    LaunchedEffect(Unit) {
        launch {
            logoAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = EaseOutCubic)
            )
        }

        delay(800)
        animationState = SplashAnimationState.ShowTagline
        delay(400)
        animationState = SplashAnimationState.ShowLoading

        delay(1200)
        onSplashCompleted()

        val destination = if (!initialFixtureId.isNullOrEmpty()) {
            Routes.FixtureDetails.createRoute(initialFixtureId)
        } else {
            Routes.Home.route
        }

        navController.navigate(destination) {
            popUpTo(Routes.Splash.route) { inclusive = true }
        }
    }
}

enum class SplashAnimationState {
    Initial,
    ShowTagline,
    ShowLoading
}

@Preview(uiMode = 1)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(
        navController = NavController(
            context = androidx.compose.ui.platform.LocalContext.current
        ), initialFixtureId = null, onSplashCompleted = {})

}
