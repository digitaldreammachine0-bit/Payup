package com.payup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayupApp(repository: WageRepository) {
    val jobs by repository.jobs().collectAsState(initial = emptyList())
    val shifts by repository.shifts().collectAsState(initial = emptyList())

    var jobName by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("25") }
    var tax by remember { mutableStateOf("20") }
    var deduction by remember { mutableStateOf("0") }

    var shiftStart by remember { mutableStateOf("2026-01-01T09:00") }
    var shiftEnd by remember { mutableStateOf("2026-01-01T17:00") }
    var breakMins by remember { mutableStateOf("30") }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(jobs) {
        if (selectedJobId == null && jobs.isNotEmpty()) selectedJobId = jobs.first().id
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Payup — Offline Work Hours & Wages", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Job")
                OutlinedTextField(jobName, { jobName = it }, label = { Text("Job name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(rate, { rate = it }, label = { Text("$/hr") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(tax, { tax = it }, label = { Text("Tax %") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(deduction, { deduction = it }, label = { Text("Deduction $") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val r = rate.toDoubleOrNull() ?: return@Button
                    val t = (tax.toDoubleOrNull() ?: return@Button) / 100.0
                    val d = deduction.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        repository.addJob(jobName.ifBlank { "Unnamed Job" }, r, 1.5, t, d)
                    }
                    jobName = ""
                }) { Text("Save Job") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log Shift (ISO local datetime)")
                Text("Selected Job: ${jobs.firstOrNull { it.id == selectedJobId }?.name ?: "None"}")
                OutlinedTextField(shiftStart, { shiftStart = it }, label = { Text("Start (e.g. 2026-01-02T09:00)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(shiftEnd, { shiftEnd = it }, label = { Text("End") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(breakMins, { breakMins = it }, label = { Text("Break minutes") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val jobId = selectedJobId ?: return@Button
                    val start = parseLocalDateTimeToEpoch(shiftStart) ?: return@Button
                    val end = parseLocalDateTimeToEpoch(shiftEnd) ?: return@Button
                    val br = breakMins.toIntOrNull() ?: 0
                    scope.launch {
                        repository.addShift(jobId, start, end, br)
                    }
                }) { Text("Save Shift") }
            }
        }

        EarningsChart(jobs, shifts, repository)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs) { job ->
                val breakdown = repository.calculateWeeklyPay(job, shifts)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(job.name, style = MaterialTheme.typography.titleMedium)
                        Text("Regular: %.2f h | OT: %.2f h".format(breakdown.regularHours, breakdown.overtimeHours))
                        Text("Gross: $%.2f | Tax: $%.2f | Deduction: $%.2f | Net: $%.2f".format(
                            breakdown.grossPay, breakdown.taxes, breakdown.deductions, breakdown.netPay
                        ))
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsChart(jobs: List<Job>, shifts: List<Shift>, repository: WageRepository) {
    val values = jobs.map { repository.calculateWeeklyPay(it, shifts).netPay.toFloat() }
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text("Earnings chart (net by job)")
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 12.dp)) {
                val barWidth = size.width / (values.size.coerceAtLeast(1) * 1.6f)
                values.forEachIndexed { index, v ->
                    val x = (index * barWidth * 1.6f) + barWidth * 0.3f
                    val h = (v / max) * (size.height * 0.9f)
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(x, size.height - h),
                        size = androidx.compose.ui.geometry.Size(barWidth, h)
                    )
                }
            }
        }
    }
}

private fun parseLocalDateTimeToEpoch(text: String): Long? = runCatching {
    LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()
