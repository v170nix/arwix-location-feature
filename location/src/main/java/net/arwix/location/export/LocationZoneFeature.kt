package net.arwix.location.export

import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.consumeAsFlow
import net.arwix.location.ui.zone.LocationZoneAction
import net.arwix.location.ui.zone.LocationZoneAdapter
import net.arwix.location.ui.zone.LocationZoneState
import net.arwix.location.ui.zone.LocationZoneViewModel
import org.threeten.bp.Instant

class LocationZoneFeature : LifecycleObserver, CoroutineScope by MainScope() {

    private lateinit var config: Config
    private lateinit var model: LocationZoneViewModel
    private lateinit var adapter: LocationZoneAdapter
    private var isSelected = false
    private var broadcastSubmitChannel: BroadcastChannel<Boolean> = ConflatedBroadcastChannel()

    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val lifecycleOwner: LifecycleOwner,
        val locationZoneFactory: ViewModelProvider.Factory,
        val timeZoneInstant: Instant = Instant.now()
    )

    fun setup(config: Config) {
        this.config = config
        adapter = LocationZoneAdapter(scope = this) { zoneId, isAuto ->
            if (isAuto) model.intent(LocationZoneAction.SelectZoneFormAuto(zoneId))
            else model.intent(LocationZoneAction.SelectZoneFromList(zoneId))
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationZoneFactory
        ).get(LocationZoneViewModel::class.java)
        model.liveState.observe(config.lifecycleOwner, ::render)
        model.nonCancelableIntent(LocationZoneAction.Init)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        broadcastSubmitChannel.cancel()
        cancel()
    }

    fun getAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder> = adapter

    fun submitAvailableAsFlow() = broadcastSubmitChannel.openSubscription().consumeAsFlow()

    suspend fun doSubmitLocation() {
        model.submit()
    }

    private fun render(state: LocationZoneState) {
        state.listZones?.run {
            adapter.setList(state.listZones)
        }
        state.autoZone?.also { autoZone ->
            adapter.setAutoState(autoZone, state.selectZoneId == null)
        }
        launch {
            delay(100)
            broadcastSubmitChannel.send(state.finishStepAvailable)
        }
        val selected = state.selectZoneId ?: return
        isSelected = true
        if (selected.fromAuto) {
            adapter.selectAuto()
        } else {
            adapter.select(selected.zoneId)
        }
    }

}