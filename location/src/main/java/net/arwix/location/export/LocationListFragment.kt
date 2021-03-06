package net.arwix.location.export

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.arwix.extension.getThemeColor
import net.arwix.location.R
import net.arwix.location.domain.LocationSettingHelper
import net.arwix.location.list.ui.LocationListAction
import net.arwix.location.list.ui.LocationListAdapter
import net.arwix.location.list.ui.LocationListState
import net.arwix.location.list.ui.LocationListViewModel
import java.time.Instant

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
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                model.onSyncAction(LocationListAction.GetAutoLocation)
            }
        }

    private val registerSettingLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                model.onSyncAction(LocationListAction.UpdateAutoLocation)
            }
        }

    private val _submitState = MutableStateFlow(false)
    val submitState = _submitState.asStateFlow()

    protected fun setup(config: Config) {
        this.config = config
        this.adapter = LocationListAdapter(
            config.timeZoneInstant,
            config.nsweStrings,
            onSecondaryColor = config.colorOnSecondary
                ?: requireContext().getThemeColor(R.attr.colorOnSecondary),
            onRequestPermission = {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onUpdateAutoLocation = {
                lifecycleScope.launch {
                    val intentSenderRequest = LocationSettingHelper.check(this@LocationListFragment)
                    if (intentSenderRequest == null) {
                        if (isRemoving || isDetached) return@launch
                        model.onSyncAction(LocationListAction.UpdateAutoLocation)
                    } else registerSettingLauncher.launch(intentSenderRequest)
                }
            },
            onSelectedListener = { item, isAuto ->
                if (isAuto) model.onLatestAction(LocationListAction.SelectFormAuto(item))
                else model.onLatestAction(LocationListAction.SelectFromCustomList(item))
            },
            onEditListener = {
                model.onSyncAction(LocationListAction.EditItem(it))
                navigateToEditItemFragment()
            },
            onDeleteListener = { item ->
                Snackbar.make(
                    requireView(),
                    R.string.location_undo_delete_message,
                    Snackbar.LENGTH_LONG
                )
                    .setAnchorView(getContextUndoDeleteView())
                    .setAction(R.string.location_undo_delete_button) {
                        model.onSyncAction(LocationListAction.UndoDeleteItem(item))
                    }.show()
                model.onSyncAction(LocationListAction.DeleteItem(item))
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationMainFactory
        ).get(LocationListViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.state.onEach(::render).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    fun getAdapter() = adapter

    abstract fun navigateToEditItemFragment()
    abstract fun getContextUndoDeleteView(): View

    fun doAddLocation() {
        lifecycleScope.launch {
            model.addNewLocation()
        }
    }

    private fun render(state: LocationListState) {
        if (state.locationList != null) {
            val autoState = when (state.autoLocationPermission) {
                LocationListState.LocationPermission.Denied -> LocationListAdapter.AutoState.Denied
                LocationListState.LocationPermission.DeniedRationale -> LocationListAdapter.AutoState.DeniedRationale
                LocationListState.LocationPermission.Allow -> LocationListAdapter.AutoState.Allow(
                    null
                )
            }
            adapter.setData(state.locationList, autoState)
        }
        _submitState.value = state.isSelected
    }

}