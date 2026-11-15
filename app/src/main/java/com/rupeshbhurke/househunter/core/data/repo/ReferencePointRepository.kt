package com.rupeshbhurke.househunter.core.data.repo

import com.rupeshbhurke.househunter.core.data.db.EntryEntity
import com.rupeshbhurke.househunter.core.data.db.ReferencePointDao
import com.rupeshbhurke.househunter.core.data.db.ReferencePointEntity
import com.rupeshbhurke.househunter.core.util.Geo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferencePointRepository @Inject constructor(
    private val dao: ReferencePointDao,
) {
    fun observeAll(): Flow<List<ReferencePointEntity>> = dao.observeAll()

    suspend fun add(name: String, lat: Double, lng: Double): Long =
        dao.insert(ReferencePointEntity(name = name.trim(), lat = lat, lng = lng))

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        /** Straight-line distance from the flat to each point, in the points' order; empty if the flat has no position. */
        fun distances(entry: EntryEntity, points: List<ReferencePointEntity>): List<Pair<ReferencePointEntity, Double>> {
            val lat = entry.lat ?: return emptyList()
            val lng = entry.lng ?: return emptyList()
            return points.map { it to Geo.distanceKm(lat, lng, it.lat, it.lng) }
        }
    }
}
