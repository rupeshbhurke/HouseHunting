package com.rupeshbhurke.househunter.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TemplateEntity::class,
        CollectionEntity::class,
        EntryEntity::class,
        EntryFieldDefEntity::class,
        FieldValueEntity::class,
        ContactEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun collectionDao(): CollectionDao
    abstract fun entryDao(): EntryDao
    abstract fun entryFieldDefDao(): EntryFieldDefDao
    abstract fun fieldValueDao(): FieldValueDao
    abstract fun contactDao(): ContactDao
}
