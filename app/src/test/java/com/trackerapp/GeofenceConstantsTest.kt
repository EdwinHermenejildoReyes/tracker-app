package com.trackerapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceConstantsTest {

    @Test
    fun `latitude is within valid range`() {
        assertTrue(GeofenceConstants.TARGET_LAT in -90.0..90.0)
    }

    @Test
    fun `longitude is within valid range`() {
        assertTrue(GeofenceConstants.TARGET_LNG in -180.0..180.0)
    }

    @Test
    fun `radius is positive`() {
        assertTrue(GeofenceConstants.RADIUS_M > 0f)
    }

    @Test
    fun `geofence id is not blank`() {
        assertTrue(GeofenceConstants.GEOFENCE_ID.isNotBlank())
    }

    @Test
    fun `prefs key constants are not blank`() {
        assertTrue(GeofenceConstants.PREFS_NAME.isNotBlank())
        assertTrue(GeofenceConstants.KEY_INSIDE.isNotBlank())
    }
}
