package com.soccertips.predictx.data.model

import com.soccertips.predictx.R

data class Category(
    val url: String,
    val name: String,
    val iconResId: Int = R.drawable.outline_add_circle_outline_24, // Default icon
    val colorHex: String? = null // Optional color override
)
