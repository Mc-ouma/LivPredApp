package com.soccertips.predcompose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
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
    var animationStarted by remember { mutableStateOf(false) }
    val alpha = animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(1000)
    )
    val scale = animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(1000)
    )
    val isDarkTheme = isSystemInDarkTheme()
    val bgColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // Customize your splash screen UI here
        AnimatedVisibility(visible = animationStarted) {
            Image(
                painter = painterResource(id = R.drawable.launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(400.dp)
                    .alpha(alpha.value)
                    .scale(scale.value)
            )
        }

    }

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(3000) // Adjust splash duration

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
