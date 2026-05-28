package com.payup

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hourlyRate: Double,
    val overtimeMultiplier: Double = 1.5,
    val taxRate: Double = 0.2,
    val deductionFlat: Double = 0.0
)

@Entity
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val breakMinutes: Int = 0
)

data class PayBreakdown(
    val regularHours: Double,
    val overtimeHours: Double,
    val grossPay: Double,
    val taxes: Double,
    val deductions: Double,
    val netPay: Double
)

@Dao
interface WageDao {
    @Query("SELECT * FROM Job ORDER BY name")
    fun jobs(): Flow<List<Job>>

    @Query("SELECT * FROM Shift ORDER BY startEpochMillis DESC")
    fun shifts(): Flow<List<Shift>>

    @Insert
    suspend fun insertJob(job: Job)

    @Insert
    suspend fun insertShift(shift: Shift)
}

@Database(entities = [Job::class, Shift::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wageDao(): WageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "payup.db"
                ).build().also { instance = it }
            }
        }
    }
}
