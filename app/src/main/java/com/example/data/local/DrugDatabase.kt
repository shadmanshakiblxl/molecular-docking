package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "docking_records")
data class DockingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pdbId: String,
    val proteinTitle: String,
    val smiles: String,
    val ligandName: String,
    val dockingScoreKcalMol: Double,
    val predictedKdFormatted: String,
    val hBondsCount: Int,
    val hydrophobicCount: Int,
    val bindingSiteMode: String,
    val pocketVolume: Double,
    val ligandEfficiency: Double,
    val lipinskiPass: Boolean,
    val clinicalSuitability: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DockingDao {
    @Query("SELECT * FROM docking_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DockingRecord>>

    @Query("SELECT * FROM docking_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): DockingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DockingRecord): Long

    @Query("DELETE FROM docking_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM docking_records")
    suspend fun clearAll()
}

@Database(entities = [DockingRecord::class], version = 1, exportSchema = false)
abstract class DrugDatabase : RoomDatabase() {
    abstract fun dockingDao(): DockingDao
}
