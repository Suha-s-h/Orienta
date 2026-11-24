package com.example

import android.app.Application
import com.example.data.DeckPreferencesStore
import com.example.data.ScreenDeckDatabase
import com.example.data.ScreenDeckRepository
import com.example.service.ScreenDeckController

class ScreenDeckApplication : Application() {

    lateinit var controller: ScreenDeckController
        private set

    override fun onCreate() {
        super.onCreate()
        val database = ScreenDeckDatabase.getInstance(this)
        val prefsStore = DeckPreferencesStore(this)
        val repository = ScreenDeckRepository(
            context = this,
            appRuleDao = database.appRuleDao(),
            preferencesStore = prefsStore
        )
        controller = ScreenDeckController(
            context = this,
            repository = repository
        )
    }
}
