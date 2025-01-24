package com.soccertips.predcompose.navigation

sealed class Routes(
    val route: String,
) {
    object Splash : Routes("splash")

    object Home : Routes("home")

    object Categories : Routes("categories")

    object Favorites : Routes("favorites")

    object ItemsList : Routes("items_list/{categoryId}") {
        fun createRoute(categoryId: String) = "items_list/$categoryId"
    }

    object FixtureDetails : Routes("fixture_details/{fixtureId}") {
        fun createRoute(fixtureId: String) = "fixture_details/$fixtureId"
    }

    object TeamDetails : Routes("team_details/{teamId}/{leagueId}/{season}") {
        fun createRoute(teamId: String, leagueId: String, season: String) = "team_details/$teamId/$leagueId/$season"
    }

}
