package net.arwix.location.export

//import org.threeten.bp.Instant
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.arwix.location.edit.zone.ui.LocationZoneAction
import net.arwix.location.edit.zone.ui.LocationZoneAdapter
import net.arwix.location.edit.zone.ui.LocationZoneState
import net.arwix.location.edit.zone.ui.LocationZoneViewModel
import java.time.Instant

open class LocationZoneFragment : Fragment() {

    private lateinit var config: Config
    private lateinit var model: LocationZoneViewModel
    private lateinit var adapter: LocationZoneAdapter
    private var isSelected = false

    private val _submitState = MutableStateFlow(false)
    val submitState: StateFlow<Boolean> = _submitState

    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val locationZoneFactory: ViewModelProvider.Factory,
        val timeZoneInstant: Instant = Instant.now()
    )

    fun setup(config: Config) {
        this.config = config
        adapter = LocationZoneAdapter(scope = lifecycleScope) { zoneId, isAuto ->
            if (isAuto) model.onMergeAction(LocationZoneAction.SelectZoneFormAuto(zoneId))
            else model.onMergeAction(LocationZoneAction.SelectZoneFromList(zoneId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationZoneFactory
        ).get(LocationZoneViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.state.onEach(::render).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    fun getAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder> = adapter

    suspend fun doSubmitLocation() {
        model.submit()
    }

    private fun render(state: LocationZoneState) {
        state.listZones?.run {
            adapter.setList(state.listZones)
        }
        state.autoZoneStatus?.also { autoZone ->
            adapter.setAutoState(autoZone, state.selectedData == null)
        }
        _submitState.value = state.finishStepAvailable
        val selected = state.selectedData ?: return
        isSelected = true
        if (selected.isAutoZone) {
            adapter.selectAuto()
        } else {
            adapter.select(selected.zone)
        }
    }

}