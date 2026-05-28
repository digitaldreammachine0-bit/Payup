# Payup (Android, offline)

A fully offline Android app to track hours and wages across multiple jobs.

## Features
- Create multiple jobs with custom hourly rate, tax rate, overtime multiplier, and flat deductions.
- Log shifts (start/end time + break minutes) entirely on-device.
- Automatic weekly pay calculation per job:
  - Regular vs overtime hours (40h threshold)
  - Gross pay
  - Taxes
  - Deductions
  - Net pay
- Earnings bar chart by job (net earnings).
- Room database for local-only persistence.

## Tech
- Kotlin + Jetpack Compose UI
- Room for offline database
- No network dependencies

## Build
```bash
./gradlew assembleDebug
```

## Notes
- Shift time input currently uses ISO local datetime (e.g. `2026-01-02T09:00`).
- This repo contains a practical MVP and can be extended with date-range filtering, export, and reminders.
