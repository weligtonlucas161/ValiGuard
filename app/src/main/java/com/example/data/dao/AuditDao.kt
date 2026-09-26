package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditItem
import com.example.data.model.AuditSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {

    @Query("SELECT * FROM audit_sessions WHERE auditType = :type AND status = 'IN_PROGRESS' ORDER BY id DESC LIMIT 1")
    fun getActiveSessionFlow(type: String): Flow<AuditSession?>

    @Query("SELECT * FROM audit_sessions WHERE auditType = :type AND status = 'IN_PROGRESS' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveSession(type: String): AuditSession?

    @Query("SELECT * FROM audit_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): AuditSession?

    @Query("SELECT * FROM audit_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedSessionsFlow(): Flow<List<AuditSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AuditSession): Long

    @Update
    suspend fun updateSession(session: AuditSession)

    @Query("DELETE FROM audit_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Audit Items
    @Query("SELECT * FROM audit_items WHERE auditSessionId = :sessionId ORDER BY id DESC")
    fun getItemsForSessionFlow(sessionId: Long): Flow<List<AuditItem>>

    @Query("SELECT * FROM audit_items WHERE auditSessionId = :sessionId ORDER BY id DESC")
    suspend fun getItemsForSession(sessionId: Long): List<AuditItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: AuditItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<AuditItem>)

    @Update
    suspend fun updateItem(item: AuditItem)

    @Delete
    suspend fun deleteItem(item: AuditItem)

    @Query("DELETE FROM audit_items WHERE auditSessionId = :sessionId")
    suspend fun clearItemsForSession(sessionId: Long)

    @Query("UPDATE audit_items SET isPresent = 1, scannedAt = :time WHERE auditSessionId = :sessionId AND barcode = :barcode")
    suspend fun markBarcodePresent(sessionId: Long, barcode: String, time: Long): Int

    @Query("UPDATE audit_items SET isPresent = 1, scannedAt = :time WHERE id = :itemId")
    suspend fun markItemPresentById(itemId: Long, time: Long)

    @Query("UPDATE audit_items SET isPresent = 0, scannedAt = null WHERE id = :itemId")
    suspend fun unmarkItemPresentById(itemId: Long)
}
