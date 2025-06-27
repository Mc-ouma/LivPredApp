package com.soccertips.predictx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun SplashScreen(
    navController: NavController,
    initialFixtureId: String?,
    onSplashCompleted: () -> Unit
) {
    // Animation state management - simplified to reduce state changes
    var animationState by remember { mutableStateOf(SplashAnimationState.Initial) }
    val density = LocalDensity.current

    // Simplified theming
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.background
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .semantics {
                contentDescription = context.getString(R.string.splash_screen)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Show logo immediately without animation delay
            Image(
                painter = painterResource(id = R.drawable.launcher),
                contentDescription = context.getString(R.string.app_logo),
                modifier = Modifier
                    .size(180.dp)
                    .scale(0.95f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tagline with slide-in animation - only appears when needed
            AnimatedVisibility(
                visible = animationState >= SplashAnimationState.ShowTagline,
                enter = fadeIn(tween(300)) +
                        slideInVertically(
                            initialOffsetY = { with(density) { 20.dp.roundToPx() } },
                            animationSpec = tween(300)
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

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator with fade-in
            AnimatedVisibility(
                visible = animationState >= SplashAnimationState.ShowLoading,
                enter = fadeIn(tween(200))
            ) {
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    // Optimized animation sequence with shorter timing
    LaunchedEffect(Unit) {
        // Show tagline after a short delay
        delay(400)
        animationState = SplashAnimationState.ShowTagline

        // Show loading indicator
        delay(200)
        animationState = SplashAnimationState.ShowLoading

        // Total splash screen time reduced to 1200ms (was 2400ms)
        delay(600)
        onSplashCompleted()

        // Navigate to next screen
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
        navController =
            NavController(context = LocalContext.current),
        initialFixtureId = null,
        onSplashCompleted = {}
    )
}
