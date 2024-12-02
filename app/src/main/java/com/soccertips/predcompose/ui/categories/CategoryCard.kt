package com.soccertips.predcompose.ui.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.soccertips.predcompose.R
import com.soccertips.predcompose.model.Category

@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
) {
    ImageListItem(name = category.name, onClick = onClick)
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImageListItem(
    name: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 26.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {

            Image(
                painter = painterResource(id = R.drawable.outline_add_circle_outline_24),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }
    }
}

@Preview
@Composable
private fun ImageListItemPreview() {
    ImageListItem(
        name = "Category Name",
        onClick = {},
    )
}
