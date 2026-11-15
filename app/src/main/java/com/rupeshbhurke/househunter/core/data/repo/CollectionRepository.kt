package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.db.CollectionDao
import com.rupeshbhurke.househunter.core.data.db.CollectionEntity
import com.rupeshbhurke.househunter.core.data.db.CollectionWithCount
import com.rupeshbhurke.househunter.core.data.db.TemplateDao
import com.rupeshbhurke.househunter.core.data.db.TemplateEntity
import com.rupeshbhurke.househunter.core.data.model.HouseHuntTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao,
    private val templateDao: TemplateDao,
) {
    fun observeCollections(): Flow<List<CollectionWithCount>> = collectionDao.observeAllWithCount()

    fun observeCollection(id: Long): Flow<CollectionEntity?> = collectionDao.observe(id)

    suspend fun create(name: String): Long {
        syncBuiltInTemplates()
        return collectionDao.insert(
            CollectionEntity(
                name = name.trim(),
                templateId = HouseHuntTemplate.ID,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun rename(id: Long, name: String) = collectionDao.rename(id, name.trim())

    suspend fun delete(id: Long) = collectionDao.delete(id)

    /**
     * Keeps the stored built-in template in sync with the one shipped in code. Called at app start
     * so fields added in an update also reach collections that already exist.
     */
    suspend fun syncBuiltInTemplates() {
        templateDao.upsert(
            TemplateEntity(
                id = HouseHuntTemplate.ID,
                name = HouseHuntTemplate.NAME,
                fieldDefsJson = FieldJson.encodeToString(HouseHuntTemplate.fields),
            ),
        )
    }
}
