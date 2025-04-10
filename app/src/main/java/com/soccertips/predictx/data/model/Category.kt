package com.soccertips.predictx.data.model

data class Category(
    val endpoint: String,
    val name: String,
    val usesAlternativeUrl: Boolean = false,
)
 object CategoryTypes{
     const val DEFAULT = false
     const val ALTERNATIVE = true
 }
