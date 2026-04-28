package com.example.scheduleiseu.data.repository.core

import android.content.Context
import com.example.scheduleiseu.data.local.db.AppDatabase
import com.example.scheduleiseu.data.local.cache.ProfilePhotoCacheDataSource
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.network.AndroidNetworkMonitor
import com.example.scheduleiseu.data.remote.cookie.MemoryCookieJar
import com.example.scheduleiseu.data.remote.parser.BsuParser
import com.example.scheduleiseu.data.session.AuthSessionStore
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.notification.LessonNotificationScheduler

object BsuCabinetDataComponent {
    val authSessionStore: AuthSessionStore by lazy { AuthSessionStore() }
    val cookieJar: MemoryCookieJar by lazy { MemoryCookieJar() }
    val parser: BsuParser by lazy { BsuParser(cookieJar = cookieJar) }

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(requireApplicationContext())
    }

    val preferences: AppPreferencesDataSource by lazy {
        AppPreferencesDataSource(requireApplicationContext())
    }

    val profilePhotoCache: ProfilePhotoCacheDataSource by lazy {
        ProfilePhotoCacheDataSource(requireApplicationContext())
    }

    val networkMonitor: NetworkMonitor by lazy {
        AndroidNetworkMonitor(requireApplicationContext())
    }

    val lessonNotificationScheduler: LessonNotificationScheduler by lazy {
        LessonNotificationScheduler(
            context = requireApplicationContext(),
            preferencesDataSource = preferences,
            scheduleCacheDao = database.scheduleCacheDao()
        )
    }

    private fun requireApplicationContext(): Context {
        return appContext
            ?: throw IllegalStateException("BsuCabinetDataComponent.initialize(context) must be called before local persistence is used")
    }
}
