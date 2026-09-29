package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.MIGRATION_1_2
import com.example.data.local.MIGRATION_2_3
import com.example.data.local.MIGRATION_3_4
import com.example.data.local.MIGRATION_4_5
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ContactCache
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
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

        preferencesRepository = PreferencesRepository(applicationContext)
        telephonyRepository = TelephonyRepository(
            applicationContext,
            database.conversationDao(),
            database.localMediaMessageDao()
        )
        contactsRepository = ContactsRepository(applicationContext)

        // Preload and register observers for high-speed contact lookup
        ContactCache.preloadContacts(this)
        ContactCache.registerObserver(this)

        NotificationHelper.createNotificationChannels(this)
    }
}
