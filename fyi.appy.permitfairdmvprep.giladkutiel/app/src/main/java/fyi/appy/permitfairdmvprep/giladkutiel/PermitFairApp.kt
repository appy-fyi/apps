package fyi.appy.permitfairdmvprep.giladkutiel

import android.app.Application
import fyi.appy.permitfairdmvprep.giladkutiel.billing.BillingRepository
import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository

class PermitFairApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var contentRepository: ContentRepository
        private set
    lateinit var progressRepository: ProgressRepository
        private set
    lateinit var billingRepository: BillingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        contentRepository = ContentRepository(this, database)
        progressRepository = ProgressRepository(database)
        billingRepository = BillingRepository(this, database)
        billingRepository.startConnection()
    }
}
