package com.abhinay.ISLEnglishGujaratiApp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.abhinay.ISLEnglishGujaratiApp.reminder.ReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderWorkerTest {
    @Test fun workerSucceedsWhetherOrNotThereIsAnythingToSend() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = (context as SanketSetuApp).store
        store.clearAll()
        store.setReminder(true)
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), runBlocking { worker.doWork() })
        store.setReminder(false)
        assertEquals(ListenableWorker.Result.success(), runBlocking { worker.doWork() })
    }
}
