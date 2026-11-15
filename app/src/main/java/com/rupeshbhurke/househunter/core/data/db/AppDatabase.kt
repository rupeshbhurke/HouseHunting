package com.rupeshbhurke.househunter.core.data.db

import androidx.room.AutoMigration
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
        AttachmentEntity::class,
        NoteEntity::class,
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun collectionDao(): CollectionDao
    abstract fun entryDao(): EntryDao
    abstract fun entryFieldDefDao(): EntryFieldDefDao
    abstract fun fieldValueDao(): FieldValueDao
    abstract fun contactDao(): ContactDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun noteDao(): NoteDao
}
