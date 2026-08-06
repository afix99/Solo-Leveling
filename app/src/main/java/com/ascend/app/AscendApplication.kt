package com.ascend.app

import android.app.Application
import com.ascend.app.data.db.AscendDatabase
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.notifications.NotificationScheduler

class AscendApplication : Application() {

    lateinit var repository: AscendRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AscendDatabase.getInstance(this)
        repository = AscendRepository(db)
        NotificationScheduler.scheduleAll(this)
    }
}
