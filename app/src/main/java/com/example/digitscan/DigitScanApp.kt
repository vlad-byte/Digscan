package com.example.digitscan

import android.app.Application
import com.example.digitscan.data.AppDatabase
import com.example.digitscan.data.Repository

class DigitScanApp : Application() {
    val repository: Repository by lazy {
        Repository(applicationContext, AppDatabase.create(applicationContext))
    }
}
