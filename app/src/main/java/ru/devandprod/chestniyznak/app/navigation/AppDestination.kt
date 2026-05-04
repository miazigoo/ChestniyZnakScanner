package ru.devandprod.chestniyznak.app.navigation

enum class AppDestination(val route: String) {
    Menu("menu"),
    Boxes("boxes/{filter}"),
    BoxLookup("box-lookup"),
    BoxDetail("box-detail/{boxId}"),
    Settings("settings"),
    ThemeSelection("theme-selection"),
    Login("login"),
    Scanner("scanner");

    companion object {
        const val FILTER_ARG = "filter"
        const val BOX_ID_ARG = "boxId"
        fun boxesRoute(filter: String): String = "boxes/$filter"
        fun boxDetailRoute(boxId: Long): String = "box-detail/$boxId"
    }
}
