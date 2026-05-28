package com.payup

import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class WageRepository(private val dao: WageDao) {
    fun jobs(): Flow<List<Job>> = dao.jobs()
    fun shifts(): Flow<List<Shift>> = dao.shifts()

    suspend fun addJob(name: String, rate: Double, otMultiplier: Double, taxRate: Double, deductions: Double) {
        dao.insertJob(
            Job(
                name = name,
                hourlyRate = rate,
                overtimeMultiplier = otMultiplier,
                taxRate = taxRate,
                deductionFlat = deductions
            )
        )
    }

    suspend fun addShift(jobId: Long, startMillis: Long, endMillis: Long, breakMinutes: Int) {
        dao.insertShift(Shift(jobId = jobId, startEpochMillis = startMillis, endEpochMillis = endMillis, breakMinutes = breakMinutes))
    }

    fun calculateWeeklyPay(job: Job, shifts: List<Shift>): PayBreakdown {
        val totalHours = shifts.filter { it.jobId == job.id }.sumOf {
            val raw = (it.endEpochMillis - it.startEpochMillis) / 3_600_000.0
            max(raw - (it.breakMinutes / 60.0), 0.0)
        }

        val regular = minOf(totalHours, 40.0)
        val overtime = max(totalHours - 40.0, 0.0)
        val gross = regular * job.hourlyRate + overtime * (job.hourlyRate * job.overtimeMultiplier)
        val taxes = gross * job.taxRate
        val deductions = job.deductionFlat
        val net = gross - taxes - deductions

        return PayBreakdown(regular, overtime, gross, taxes, deductions, net)
    }
}
