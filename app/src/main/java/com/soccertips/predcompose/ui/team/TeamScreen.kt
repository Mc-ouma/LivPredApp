package com.soccertips.predcompose.ui.team


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter

@Composable
fun StatisticsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Team Logo and Overview
        Card(modifier = Modifier.fillMaxWidth(), elevation =CardDefaults.cardElevation(defaultElevation = 4.dp)){
            TeamOverview()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Goals Stats Table
        Card(modifier = Modifier.fillMaxWidth(), elevation =CardDefaults.cardElevation(defaultElevation = 4.dp)){
            GoalsStatsTable()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conceded Goals Stats Table
        Card(modifier = Modifier.fillMaxWidth(), elevation =CardDefaults.cardElevation(defaultElevation = 4.dp)){
            ConcededGoalsStatsTable()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cards Stats Table
        Card(modifier = Modifier.fillMaxWidth(), elevation =CardDefaults.cardElevation(defaultElevation = 4.dp)){
            CardsStatsTable()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Penalties Table
        Card(modifier = Modifier.fillMaxWidth(), elevation =CardDefaults.cardElevation(defaultElevation = 4.dp)){
            PenaltiesTable()
        }
    }
}

@Composable
fun TeamOverview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter("https://media.api-sports.io/football/teams/33.png"),
                contentDescription = "Manchester United Logo",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("Manchester United", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Season: 2019 | Premier League", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun GoalsStatsTable() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Goals Scored (Total: 66)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableHeader("Location", "Goals", "Average")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Home", "40", "2.1")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Away", "26", "1.4")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Overall", "66", "1.7")
                }
            }
        }
    }
}

@Composable
fun ConcededGoalsStatsTable() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Goals Conceded (Total: 36)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableHeader("Location", "Goals", "Average")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Home", "17", "0.9")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Away", "19", "1.0")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Overall", "36", "0.9")
                }
            }
        }
    }
}

@Composable
fun CardsStatsTable() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Yellow Cards", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableHeader("Time Period", "Yellow Cards")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("0-15 mins", "5")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("16-30 mins", "5")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("31-45 mins", "16")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("46-60 mins", "12")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("61-75 mins", "14")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("76-90 mins", "21")
                }
            }
        }
    }
}

@Composable
fun PenaltiesTable() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Penalties", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableHeader("Penalty Type", "Count")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Scored", "10")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableRow("Missed", "0")
                }
            }
        }
    }
}

@Composable
fun TableHeader(vararg titles: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        titles.forEach { title ->
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TableRow(vararg values: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    StatisticsScreen()
}
