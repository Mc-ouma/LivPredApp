package com.soccertips.predcompose.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.soccertips.predcompose.R

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
       // LottieLoader(resId = R.raw.loader)
    }
}

@Composable
fun ErrorIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        LottieLoader(resId = R.raw.animation)
    }
    
}
@Preview()
@Composable
fun LoadingIndicatorPreview() {
    LoadingIndicator()

}
@Preview()
@Composable
fun ErrorIndicatorPreview() {
    ErrorIndicator()
}
