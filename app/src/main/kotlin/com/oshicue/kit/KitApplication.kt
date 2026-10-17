package com.oshicue.kit

import android.app.Application
import android.content.Context
import com.oshicue.kit.data.AgendaStore
import com.oshicue.kit.data.DiaryStore
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.i18n.L10n

class KitApplication : Application() {
    lateinit var scheduleStore: ScheduleStore
        private set
    lateinit var ledgerStore: LedgerStore
        private set
    lateinit var diaryStore: DiaryStore
        private set
    lateinit var agendaStore: AgendaStore
        private set

    override fun onCreate() {
        super.onCreate()
        L10n.attach(this)
        scheduleStore = ScheduleStore(this)
        ledgerStore = LedgerStore(this)
        diaryStore = DiaryStore(this)
        agendaStore = AgendaStore(this)
    }

    companion object {
        fun stores(context: Context): KitApplication = context.applicationContext as KitApplication
    }
}
