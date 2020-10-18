package net.arwix.location.export

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.arwix.extension.getThemeColor
import net.arwix.location.R
import net.arwix.location.domain.LocationPermissionHelper
import net.arwix.location.domain.LocationSettingHelper
import net.arwix.location.ui.list.LocationListAction
import net.arwix.location.ui.list.LocationListAdapter
import net.arwix.location.ui.list.LocationListState
import net.arwix.location.ui.list.LocationListViewModel
import org.threeten.bp.Instant

abstract class LocationListFragment : Fragment() {

    @Suppress("ArrayInDataClass")
    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val locationMainFactory: ViewModelProvider.Factory,
        val timeZoneInstant: Instant = Instant.now(),
        val nsweStrings: Array<String> = arrayOf("N", "S", "W", "E"),
        val colorOnSecondary: Int? = null
    )

    private lateinit var config: Config
    private lateinit var model: LocationListViewModel
    private lateinit var adapter: LocationListAdapter

    private val _submitState = MutableStateFlow(false)
    val submitState: StateFlow<Boolean> = _submitState

    protected fun setup(config: Config) {
        this.config = config
        this.adapter = LocationListAdapter(
            config.timeZoneInstant,
            config.nsweStrings,
            onSecondaryColor = config.colorOnSecondary
                ?: requireContext().getThemeColor(R.attr.colorOnSecondary),
            onRequestPermission = {
                LocationPermissionHelper.requestPermissionRationale(this, force = true)
            },
            onUpdateAutoLocation = {
                lifecycleScope.launch {
                    if (!LocationSettingHelper.check(this@LocationListFragment)) {
                        if (isRemoving || isDetached) return@launch
                        model.nonCancelableIntent(LocationListAction.UpdateAutoLocation)
                    }
                }
            },
            onSelectedListener = { item, isAuto ->
                if (isAuto) model.intent(LocationListAction.SelectFormAuto(item))
                else model.intent(LocationListAction.SelectFromCustomList(item))
            },
            onEditListener = {
                model.nonCancelableIntent(LocationListAction.EditItem(it))
                navigateToEditItemFragment()
            },
            onDeleteListener = {
                model.nonCancelableIntent(LocationListAction.DeleteItem(it))
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationMainFactory
        ).get(LocationListViewModel::class.java)
        model.liveState.observe(this, Observer(::render))
    }

    suspend fun commitSelectedItem() = model.commitSelectedItem()

    fun getAdapter() = adapter

    abstract fun navigateToEditItemFragment()

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
            _submitState.value = true
        } else {
            adapter.deselect()
            _submitState.value = false
        }
    }

}