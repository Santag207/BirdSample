package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Project::class,
        Site::class,
        Sampling::class,
        Observation::class,
        FormTemplate::class,
        SyncLog::class,
        BirdArticle::class,
        SamplingSession::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun siteDao(): SiteDao
    abstract fun samplingDao(): SamplingDao
    abstract fun observationDao(): ObservationDao
    abstract fun formTemplateDao(): FormTemplateDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun birdArticleDao(): BirdArticleDao
    abstract fun samplingSessionDao(): SamplingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bird_sample_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
