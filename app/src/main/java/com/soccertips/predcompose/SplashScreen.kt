package com.soccertips.predcompose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predcompose.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    initialFixtureId: String?,
    onSplashCompleted: () -> Unit
) {
    // Animation states
    var animationStarted by remember { mutableStateOf(true) }
    var showTagline by remember { mutableStateOf(false) }

    // Ensure visibility with stronger contrast
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.background
    }

    // More visible gradient
    val gradientColors = if (isDarkTheme) {
        listOf(backgroundColor, backgroundColor.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.4f))
    } else {
        listOf(
            primaryColor.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.primaryContainer,
            backgroundColor
        )
    }

    // Logo animations with smoother timing
    val logoAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(800), label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (animationStarted) 1.1f else 0.9f,
        animationSpec = tween(1000), label = "logoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Content column with more visible elements
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Fallback text in case image doesn't load
            Text(
                text = "Soccer Tips Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(if (logoAlpha < 0.5f) 0.7f else 0f)
            )

            // App logo with safer animation
            Image(
                painter = painterResource(id = R.drawable.launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(200.dp)
                    .alpha(logoAlpha)
                    .scale(logoScale)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator
            CircularProgressIndicator(
                color = primaryColor,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(logoAlpha),
                strokeWidth = 3.dp
            )
        }
    }

    // Longer delay before navigation
    LaunchedEffect(Unit) {
        animationStarted = true
        delay(1000)
        showTagline = true
        delay(2500) // Longer delay to ensure visibility

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

@Preview(uiMode = 1)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(
        navController = NavController(
            context = androidx.compose.ui.platform.LocalContext.current
        ), initialFixtureId = null, onSplashCompleted = {})

}
