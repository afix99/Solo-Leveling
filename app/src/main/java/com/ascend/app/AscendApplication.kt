package com.ascend.app

import android.app.Application
import com.ascend.app.data.db.AscendDatabase
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AscendApplication : Application() {

    lateinit var repository: AscendRepository
        private set

    /** For one-off startup work that must outlive any single screen. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = AscendDatabase.getInstance(this)
        repository = AscendRepository(db)
        applicationScope.launch { repository.ensureProfileExists() }
        NotificationScheduler.scheduleAll(this)
    }
}
