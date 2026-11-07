package com.wault.app

import android.app.Application
import com.wault.app.data.WaultRepository
import com.wault.app.domain.SessionDirector
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt application root. Bootstraps the demo vault: seeds the in-memory
 * store from the mirrored dataset, then creates + starts the hot MockEngine
 * sessions through the SessionDirector.
 */
@HiltAndroidApp
class WaultApp : Application() {

    @Inject
    lateinit var repository: WaultRepository

    @Inject
    lateinit var sessionDirector: SessionDirector

    override fun onCreate() {
        super.onCreate()
        repository.bootstrap()
        sessionDirector.bootstrapDemoAccounts()
    }
}
