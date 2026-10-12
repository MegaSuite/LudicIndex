package com.ludicindex.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ludicindex.app.data.local.AppDatabase

/**
 * In-memory database for JVM tests.
 *
 * The direct executors matter: Room's suspend queries and the invalidation tracker would otherwise
 * hop to a real thread pool, which makes `advanceUntilIdle()` return before a ViewModel's coroutine
 * resumes and turns those tests flaky.
 */
fun createTestDatabase(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(Runnable::run)
        .setTransactionExecutor(Runnable::run)
        .build()
}
