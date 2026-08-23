package fyi.appy.taponceremote.giladkutiel

import android.app.Application
import fyi.appy.taponceremote.giladkutiel.data.db.AppDatabase
import fyi.appy.taponceremote.giladkutiel.data.discovery.DefaultDiscoveryRepository
import fyi.appy.taponceremote.giladkutiel.data.discovery.DiscoveryRepository
import fyi.appy.taponceremote.giladkutiel.data.ir.IrProfileRepository
import fyi.appy.taponceremote.giladkutiel.data.ir.SystemIrTransmitter

class TapOnceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val discoveryRepository: DiscoveryRepository by lazy { DefaultDiscoveryRepository(this) }
    val irProfileRepository: IrProfileRepository by lazy { IrProfileRepository(this) }
    val irTransmitter: SystemIrTransmitter by lazy { SystemIrTransmitter(this) }
}
