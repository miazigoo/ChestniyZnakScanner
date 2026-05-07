package ru.devandprod.chestniyznak.core.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `remote version greater than current returns true`() {
        assertTrue(VersionComparator.isRemoteNewer("1.0.0", "1.0.1"))
        assertTrue(VersionComparator.isRemoteNewer("1.2.9", "1.3.0"))
    }

    @Test
    fun `same or lower remote version returns false`() {
        assertFalse(VersionComparator.isRemoteNewer("1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isRemoteNewer("1.2.0", "1.1.9"))
    }
}
