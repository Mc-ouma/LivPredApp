package com.soccertips.predcompose.navigation

sealed class Routes(
    val route: String,
) {
    object Categories : Routes("categories")

    object ItemsList : Routes("items_list/{categoryId}") {
        fun createRoute(categoryId: String) = "items_list/$categoryId"
    }

    object FixtureDetails : Routes("fixture_details/{fixtureId}") {
        fun createRoute(fixtureId: String) = "fixture_details/$fixtureId"
    }

}
