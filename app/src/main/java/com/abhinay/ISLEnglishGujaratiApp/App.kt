package com.abhinay.ISLEnglishGujaratiApp

import android.app.Application
import com.abhinay.ISLEnglishGujaratiApp.data.UserStore

class SanketSetuApp : Application() {
    val store by lazy { UserStore(this) }
}
