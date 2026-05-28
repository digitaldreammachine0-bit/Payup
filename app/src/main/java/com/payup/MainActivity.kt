package com.payup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WageRepository(AppDatabase.get(this).wageDao())
        setContent { PayupApp(repository) }
    }
}
