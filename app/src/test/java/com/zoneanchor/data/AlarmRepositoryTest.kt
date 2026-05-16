package com.zoneanchor.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zoneanchor.data.alarms.AlarmRepository
import com.zoneanchor.model.ZonedAlarm
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: AlarmRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = AlarmRepository(database.alarmDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun modelRoundTripsThroughEntityMapping() = runTest {
        repository.upsert(
            ZonedAlarm(
                id = 99,
                label = "  Wake up  ",
                zoneId = "Not/AZone",
                hour = 26,
                minute = -1,
                daysOfWeekMask = 0,
                soundMode = "beep",
                vibrate = false,
                enabled = true,
                nextTriggerAtMillis = 987L,
            ),
        )

        val alarm = repository.allOnce().single()
        assertThat(alarm.id).isEqualTo(ZonedAlarm.FIRST_ID)
        assertThat(alarm.label).isEqualTo("Wake up")
        assertThat(alarm.zoneId).isEqualTo(ZonedAlarm.DEFAULT_ZONE_ID)
        assertThat(alarm.hour).isEqualTo(23)
        assertThat(alarm.minute).isEqualTo(0)
        assertThat(alarm.daysOfWeekMask).isEqualTo(ZonedAlarm.ALL_DAYS)
        assertThat(alarm.soundMode).isEqualTo(ZonedAlarm.SOUND_DEFAULT)
        assertThat(alarm.vibrate).isFalse()
        assertThat(alarm.enabled).isTrue()
    }
}
