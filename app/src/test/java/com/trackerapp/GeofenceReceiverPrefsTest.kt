package com.trackerapp

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeofenceReceiverPrefsTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences(GeofenceConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `initial state is not inside geofence`() {
        assertFalse(prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false))
    }

    @Test
    fun `enter transition marks device as inside`() {
        prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, true).commit()
        assertTrue(prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false))
    }

    @Test
    fun `exit transition clears inside flag`() {
        prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, true).commit()
        prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, false).commit()
        assertFalse(prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false))
    }

    @Test
    fun `duplicate enter is blocked by inside flag`() {
        // Primera entrada — no estaba adentro
        val alreadyInside = prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false)
        assertFalse(alreadyInside)
        prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, true).commit()

        // Segunda entrada — debe quedar bloqueada por el flag
        assertTrue(prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false))
    }
}
