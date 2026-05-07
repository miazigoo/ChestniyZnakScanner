package ru.devandprod.chestniyznak.core.runtime

object VersionComparator {
    fun isRemoteNewer(current: String, remote: String): Boolean {
        val currentParts = parse(current)
        val remoteParts = parse(remote)
        val maxSize = maxOf(currentParts.size, remoteParts.size)
        for (index in 0 until maxSize) {
            val currentPart = currentParts.getOrElse(index) { 0 }
            val remotePart = remoteParts.getOrElse(index) { 0 }
            if (remotePart > currentPart) return true
            if (remotePart < currentPart) return false
        }
        return false
    }

    private fun parse(version: String): List<Int> = version
        .trim()
        .split('.')
        .mapNotNull { it.toIntOrNull() }
}
