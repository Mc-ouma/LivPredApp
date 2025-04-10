package com.soccertips.predictx.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.compositionLocalOf

// Define CompositionLocal for CardColors
val LocalCardColors = compositionLocalOf<CardColors> {
    error("No CardColors provided!")
}

// Define CompositionLocal for CardElevation
val LocalCardElevation = compositionLocalOf<CardElevation> {
    error("No CardElevation provided!")
}