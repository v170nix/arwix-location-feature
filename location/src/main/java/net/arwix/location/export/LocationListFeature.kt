package net.arwix.location.export

import android.util.Log
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
import net.arwix.extension.runWeak
import net.arwix.extension.weak
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
        val nsweStrings: Array<String> = arrayOf("N", "S", "W", "E")
    )

    data class ResultSetup(
        val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
        val flowSubmitAvailable: Flow<Boolean>
    )

    private lateinit var config: Config
    private lateinit var model: LocationListViewModel
    private lateinit var adapter: LocationListAdapter
    private var broadcastSubmitChannel: BroadcastChannel<Boolean> = ConflatedBroadcastChannel()

    fun setup(config: Config, fragment: Fragment): ResultSetup {
        this.config = config
        val weakFragment = fragment.weak()
        this.adapter = LocationListAdapter(
            config.nsweStrings,
            Instant.now(),
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
            },
            onDeleteListener = {
                model.nonCancelableIntent(LocationListAction.DeleteItem(it))
            }
        )
        return ResultSetup(adapter, broadcastSubmitChannel.openSubscription().consumeAsFlow())
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationMainFactory
        ).get(LocationListViewModel::class.java)
        model.liveState.observe(config.lifecycleOwner, this::render)
//        model.listItems.observe(config.lifecycleOwner, this::setData)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        broadcastSubmitChannel.cancel()
        cancel()
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
        Log.e("lmf", state.toString())
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