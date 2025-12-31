package com.soccertips.predictx.ui.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.Category

@Composable
fun CategoryCard(
        category: Category,
        onClick: () -> Unit,
) {
        ImageListItem(
                name = category.name,
                iconResId = category.iconResId,
                colorHex = category.colorHex,
                onClick = onClick
        )
}

@Composable
fun ImageListItem(
        name: String,
        iconResId: Int = R.drawable.outline_add_circle_outline_24,
        colorHex: String? = null,
        onClick: () -> Unit,
) {
        val cardElevation = CardDefaults.elevatedCardElevation(2.dp)

        // Use custom color if provided or extract from icon
        val iconTint = colorHex?.let { Color(it.toColorInt()) } ?: MaterialTheme.colorScheme.primary

        // Create card color based on the icon tint with 15% opacity for the container
        val containerColor = iconTint.copy(alpha = 0.15f)
        val customCardColors =
                CardDefaults.cardColors(containerColor = containerColor, contentColor = iconTint)

        ElevatedCard(
                onClick = onClick,
                colors = customCardColors,
                elevation = cardElevation,
                modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
        ) {
                Column(Modifier.fillMaxWidth()) {
                        Box(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .padding(24.dp)
                        ) {
                                // Make the main icon larger and centered in the card
                                Image(
                                        painter = painterResource(id = iconResId),
                                        contentDescription = null,
                                        modifier = Modifier
                                                .size(112.dp)
                                                .align(Alignment.Center),
                                        contentScale = ContentScale.Fit,
                                        colorFilter = ColorFilter.tint(iconTint)
                                )
                        }

                        Text(
                                text = name,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium,
                                color = iconTint,
                                modifier =
                                        Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp)
                                                .wrapContentWidth(Alignment.CenterHorizontally),
                        )
                }
        }
}

@Preview(showBackground = true)
@Composable
private fun CategoryCardPreview() {
        // Wrap preview in MaterialTheme so typography and color scheme are available
        MaterialTheme {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        val sample1 = Category(
                                url = "",
                                name = "Category 1",
                                iconResId = R.drawable.outline_add_circle_outline_24,
                                colorHex = "#1E88E5"
                        )

                        val sample2 = Category(
                                url = "",
                                name = "Category 2",
                                iconResId = R.drawable.outline_add_circle_outline_24,
                                colorHex = "#F4511E"
                        )

                        val sample3 = Category(
                                url = "",
                                name = "Default Color",
                                iconResId = R.drawable.outline_add_circle_outline_24,
                                colorHex = null
                        )

                        // Show different states
                        CategoryCard(category = sample1, onClick = {})
                        CategoryCard(category = sample2, onClick = {})
                        CategoryCard(category = sample3, onClick = {})
                }
        }
}