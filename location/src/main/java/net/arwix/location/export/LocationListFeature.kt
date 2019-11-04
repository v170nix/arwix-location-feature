package net.arwix.location.export

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import net.arwix.extension.getThemeColor
import net.arwix.extension.runWeak
import net.arwix.extension.weak
import net.arwix.location.R
import net.arwix.location.domain.LocationPermissionHelper
import net.arwix.location.domain.LocationSettingHelper
import net.arwix.location.ui.list.LocationListAction
import net.arwix.location.ui.list.LocationListAdapter
import net.arwix.location.ui.list.LocationListState
import net.arwix.location.ui.list.LocationListViewModel
import org.threeten.bp.Instant

class LocationListFeature : LifecycleObserver, CoroutineScope by MainScope() {

    @Suppress("ArrayInDataClass")
    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val lifecycleOwner: LifecycleOwner,
        val locationMainFactory: ViewModelProvider.Factory,
        val timeZoneInstant: Instant = Instant.now(),
        val nsweStrings: Array<String> = arrayOf("N", "S", "W", "E"),
        val onSecondaryColor: Int? = null,
        val onEditView: (() -> Unit)? = null
    )

    data class Result(
        val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
        val flowSubmitAvailable: Flow<Boolean>
    )

    private lateinit var config: Config
    private lateinit var model: LocationListViewModel
    private lateinit var adapter: LocationListAdapter
    private var broadcastSubmitChannel: BroadcastChannel<Boolean> = ConflatedBroadcastChannel()

    fun setup(config: Config, fragment: Fragment): Result {
        this.config = config
        val weakFragment = fragment.weak()
        this.adapter = LocationListAdapter(
            config.timeZoneInstant,
            config.nsweStrings,
            onSecondaryColor = config.onSecondaryColor
                ?: fragment.requireContext().getThemeColor(R.attr.colorOnSecondary),
            onRequestPermission = {
                weakFragment.runWeak {
                    LocationPermissionHelper.requestPermissionRationale(this, force = true)
                }
            },
            onUpdateAutoLocation = {
                launch {
                    weakFragment.runWeak {
                        if (!LocationSettingHelper.check(this)) {
                            if (this.isRemoving || this.isDetached) return@launch
                            model.nonCancelableIntent(LocationListAction.UpdateAutoLocation)
                        }
                    }
                }
            },
            onSelectedListener = { item, isAuto ->
                if (isAuto) model.intent(LocationListAction.SelectFormAuto(item))
                else model.intent(LocationListAction.SelectFromCustomList(item))
            },
            onEditListener = {
                model.nonCancelableIntent(LocationListAction.EditItem(it))
                this.config.onEditView?.run { invoke() }
            },
            onDeleteListener = {
                model.nonCancelableIntent(LocationListAction.DeleteItem(it))
            }
        )
        return Result(adapter, broadcastSubmitChannel.openSubscription().consumeAsFlow())
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationMainFactory
        ).get(LocationListViewModel::class.java)
        model.liveState.observe(config.lifecycleOwner, ::render)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        broadcastSubmitChannel.cancel()
        cancel()
    }

    suspend fun commitSelectedItem() = model.commitSelectedItem()


    fun doAddLocation() {
        model.nonCancelableIntent(LocationListAction.AddItem)
    }

    fun doActivityResult(requestCode: Int, resultCode: Int) {
        if (LocationSettingHelper.onActivityResult(requestCode, resultCode)) {
            model.nonCancelableIntent(LocationListAction.UpdateAutoLocation)
        }
    }

    fun doRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (LocationPermissionHelper.onRequestPermissionsResult(requestCode, grantResults)) {
            model.nonCancelableIntent(LocationListAction.GetAutoLocation)
        }
    }


    private fun render(state: LocationListState) {
        when (state.autoLocationPermission) {
            LocationListState.LocationPermission.Denied -> adapter.setAutoState(
                LocationListAdapter.AutoState.Denied
            )
            LocationListState.LocationPermission.DeniedRationale -> adapter.setAutoState(
                LocationListAdapter.AutoState.DeniedRationale
            )
            LocationListState.LocationPermission.Allow -> adapter.setAutoState(
                LocationListAdapter.AutoState.Allow(state.autoLocationTimeZoneData)
            )
        }
        if (state.customList != null) {
            adapter.setData(state.customList)
        }
        if (state.selectedItem != null) {
            adapter.select(state.selectedItem.data, state.selectedItem.isAuto)
            broadcastSubmitChannel.offer(true)
        } else {
            adapter.deselect()
            broadcastSubmitChannel.offer(false)
        }
    }


}