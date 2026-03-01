package com.ap0n.headache.di

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ap0n.headache.data.local.FactorEntity
import com.ap0n.headache.data.local.HeadacheDao
import com.ap0n.headache.data.local.HeadacheEntity
import com.ap0n.headache.data.local.QuestionEntity
import com.ap0n.headache.domain.model.QuestionType
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext app: Context,
        provider: Provider<HeadacheDao>
    ): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, "headache_db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed Data
                    CoroutineScope(Dispatchers.IO).launch {
                        seedData(provider.get())
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideHeadacheDao(db: AppDatabase): HeadacheDao = db.headacheDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    private suspend fun seedData(dao: HeadacheDao) {
        val random = java.util.Random()
        val headaches = mutableListOf<HeadacheEntity>()
        val factors = mutableListOf<FactorEntity>()

        for (i in 0..20) {
            val id = UUID.randomUUID().toString()
            val severity = random.nextInt(10) + 1
            // Simulate: Less sleep = Higher severity
            val sleepHours = 4 + random.nextInt(6)
            val simulatedSeverity = if (sleepHours < 6) random.nextInt(4) + 6 else random.nextInt(5)

            val ts = System.currentTimeMillis() - (i * 86400000L) // Past days

            headaches.add(HeadacheEntity(id, ts, simulatedSeverity, 60, "Seeded"))
            factors.add(
                FactorEntity(
                    UUID.randomUUID().toString(),
                    id,
                    "sleep_hours",
                    sleepHours.toString(),
                    QuestionType.NUMERIC,
                    ts
                )
            )
            factors.add(
                FactorEntity(
                    UUID.randomUUID().toString(),
                    id,
                    "hydration",
                    (random.nextBoolean()).toString(),
                    QuestionType.BOOLEAN,
                    ts
                )
            )
        }
        dao.insertHeadache(headaches.first()) // Trigger DB create
        headaches.forEach { dao.insertHeadache(it) }
        dao.insertFactors(factors)
    }
}

@Database(
    entities = [HeadacheEntity::class, FactorEntity::class, QuestionEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun headacheDao(): HeadacheDao
}