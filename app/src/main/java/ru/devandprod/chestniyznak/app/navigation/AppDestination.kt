package ru.devandprod.chestniyznak.app.navigation

enum class AppDestination(val route: String) {
    Menu("menu"),
    Boxes("boxes/{filter}"),
    Settings("settings"),
    ThemeSelection("theme-selection"),
    Login("login"),
    Scanner("scanner");

    companion object {
        const val FILTER_ARG = "filter"
        fun boxesRoute(filter: String): String = "boxes/$filter"
    }
}
