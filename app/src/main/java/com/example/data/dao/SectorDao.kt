package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Sector
import kotlinx.coroutines.flow.Flow

@Dao
interface SectorDao {
    @Query("SELECT * FROM sectors ORDER BY isCustom ASC, name ASC")
    fun getAllSectors(): Flow<List<Sector>>

    @Query("SELECT * FROM sectors ORDER BY isCustom ASC, name ASC")
    suspend fun getAllSectorsSync(): List<Sector>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSector(sector: Sector): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSectors(sectors: List<Sector>)

    @Delete
    suspend fun deleteSector(sector: Sector)

    @Query("DELETE FROM sectors WHERE name = :name")
    suspend fun deleteSectorByName(name: String)

    @Query("SELECT COUNT(*) FROM sectors")
    suspend fun getCount(): Int
}
