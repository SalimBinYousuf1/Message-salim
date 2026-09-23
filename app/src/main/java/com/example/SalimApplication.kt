package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ContactsRepository
import com.example.data.repository.TelephonyRepository
import com.example.telephony.NotificationHelper

class SalimApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var telephonyRepository: TelephonyRepository
        private set

    lateinit var contactsRepository: ContactsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "salim_messages.db"
        ).fallbackToDestructiveMigration().build()

        preferencesRepository = PreferencesRepository(applicationContext)
        telephonyRepository = TelephonyRepository(applicationContext, database.conversationDao())
        contactsRepository = ContactsRepository(applicationContext)

        NotificationHelper.createNotificationChannels(this)
    }
}
