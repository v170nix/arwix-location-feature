package net.arwix.location.export

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _submitState = MutableStateFlow(false)
    val submitState: StateFlow<Boolean> = _submitState

    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val lifecycleOwner: LifecycleOwner,
        val locationZoneFactory: ViewModelProvider.Factory,
        val fragmentManager: FragmentManager,
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
        cancel()
    }

    fun getAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder> = adapter

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
        _submitState.value = state.finishStepAvailable
        val selected = state.selectZoneId ?: return
        isSelected = true
        if (selected.fromAuto) {
            adapter.selectAuto()
        } else {
            adapter.select(selected.zoneId)
        }
    }

}