package com.rupeshbhurke.househunter.core.data.di

import android.content.Context
import androidx.room.Room
import com.rupeshbhurke.househunter.core.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "househunter.db").build()

    @Provides fun templateDao(db: AppDatabase) = db.templateDao()
    @Provides fun collectionDao(db: AppDatabase) = db.collectionDao()
    @Provides fun entryDao(db: AppDatabase) = db.entryDao()
    @Provides fun entryFieldDefDao(db: AppDatabase) = db.entryFieldDefDao()
    @Provides fun fieldValueDao(db: AppDatabase) = db.fieldValueDao()
    @Provides fun contactDao(db: AppDatabase) = db.contactDao()
}
