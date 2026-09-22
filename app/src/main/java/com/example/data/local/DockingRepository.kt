package com.example.data.local

import kotlinx.coroutines.flow.Flow

class DockingRepository(private val dao: DockingDao) {
    val allRecords: Flow<List<DockingRecord>> = dao.getAllRecords()

    suspend fun insertRecord(record: DockingRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun getRecordById(id: Long): DockingRecord? {
        return dao.getRecordById(id)
    }

    suspend fun deleteRecordById(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
