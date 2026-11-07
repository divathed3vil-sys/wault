package com.wault.app.di

import com.wault.app.data.InMemoryStore
import com.wault.app.data.WaultRepository
import com.wault.app.domain.InboxBus
import com.wault.app.domain.SessionDirector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** App-lifetime coroutine scope for fire-and-forget wiring (never UI work). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun inMemoryStore(): InMemoryStore = InMemoryStore()

    @Provides
    @Singleton
    fun inboxBus(@ApplicationScope scope: CoroutineScope): InboxBus = InboxBus(scope)

    @Provides
    @Singleton
    fun sessionDirector(bus: InboxBus): SessionDirector = SessionDirector(bus)

    @Provides
    @Singleton
    fun waultRepository(
        store: InMemoryStore,
        bus: InboxBus,
        director: SessionDirector,
        @ApplicationScope scope: CoroutineScope,
    ): WaultRepository = WaultRepository(store, bus, director, scope)
}
