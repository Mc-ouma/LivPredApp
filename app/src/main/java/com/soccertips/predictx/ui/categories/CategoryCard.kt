package com.soccertips.predictx.ui.categories

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.ui.theme.PredictXTheme

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
    val cardElevation = LocalCardElevation.current

    // Use custom color if provided or extract from icon
    val iconTint = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary

    // Create card color based on the icon tint with 15% opacity for the container
    val containerColor = iconTint.copy(alpha = 0.15f)
    val customCardColors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = iconTint
    )

    Card(
        onClick = onClick,
        colors = customCardColors,
        elevation = cardElevation,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 26.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(24.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(iconTint)
            )

            Text(
                text = name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                color = iconTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Preview("Dark Theme", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImageListItemPreview() {
    PredictXTheme {
        ImageListItem(
            name = "Category Name",
            iconResId = R.drawable.outline_add_circle_outline_24,
            colorHex = "#4CAF50",
            onClick = {},
        )
    }
}


