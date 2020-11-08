package net.arwix.location.export

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.arwix.extension.gone
import net.arwix.extension.hideSoftInputFromWindow
import net.arwix.extension.visible
import net.arwix.location.R
import net.arwix.location.edit.data.PlaceAutocompleteResult
import net.arwix.location.edit.position.ui.LocationPositionAction
import net.arwix.location.edit.position.ui.LocationPositionState
import net.arwix.location.edit.position.ui.LocationPositionViewModel

abstract class LocationPositionFragment : Fragment(), OnMapReadyCallback {

    @Suppress("ArrayInDataClass")
    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val locationPositionFactory: ViewModelProvider.Factory,
        val mapFragment: SupportMapFragment,
        val placeKey: String,
        val errorFormatString: String = mapFragment.resources.getString(R.string.location_error_range)
    )

    private lateinit var config: Config
    private lateinit var model: LocationPositionViewModel
    private var markerStateFlow: MutableStateFlow<Pair<LatLng, CameraPosition?>?> =
        MutableStateFlow(null)
    private var googleMap: GoogleMap? = null
    private val placeFields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.LAT_LNG,
        Place.Field.ADDRESS_COMPONENTS,
        Place.Field.ADDRESS
    )

    private val placeAutocompleteLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data ?: return@registerForActivityResult

            val place = when (activityResult.resultCode) {
                Activity.RESULT_OK -> PlaceAutocompleteResult.Ok(
                    Autocomplete.getPlaceFromIntent(data)
                )
                AutocompleteActivity.RESULT_ERROR -> PlaceAutocompleteResult.Error(
                    Autocomplete.getStatusFromIntent(data)
                )
                Activity.RESULT_CANCELED -> PlaceAutocompleteResult.Canceled
                else -> return@registerForActivityResult
            }.getPlaceInResult() ?: return@registerForActivityResult

            model.nextLatestAction(
                LocationPositionAction.ChangeFromPlace(
                    place,
                    getCameraPosition(place.latLng)
                )
            )
        }

    abstract var inputView: EditPositionView

    private val _submitState = MutableStateFlow(false)
    val submitState = _submitState.asStateFlow()

    protected fun setup(config: Config) {
        this.config = config
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationPositionFactory
        ).get(LocationPositionViewModel::class.java)
        model.state.onEach(::render).launchIn(viewLifecycleOwner.lifecycleScope)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, config.placeKey)
        }
        config.mapFragment.getMapAsync(this)

        val googleLogo = config.mapFragment.requireView().findViewWithTag<View>("GoogleWatermark")
        val glLayoutParams = googleLogo.layoutParams as RelativeLayout.LayoutParams
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
        googleLogo.layoutParams = glLayoutParams
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputView.apply {
            placeSearchText.setOnClickListener {
                placeAutocompleteLauncher.launch(
                    Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, placeFields)
                        .build(requireContext())
                )
            }
            latitudeEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) actionFromInput()
                false
            }
            longitudeEditText.setOnEditorActionListener { v, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE,
                    EditorInfo.IME_ACTION_NEXT,
                    EditorInfo.IME_ACTION_PREVIOUS -> with(v) {
                        actionFromInput()
                        clearFocus()
                        hideSoftInputFromWindow()
                        true
                    }
                    else -> false
                }
            }
            expandInputButton.setOnClickListener {
                if (inputView.latitudeInputLayout.visibility == View.VISIBLE) {
                    expandInputButton.setImageResource(R.drawable.ic_location_feature_expand_less)
                    latitudeInputLayout.gone()
                    longitudeInputLayout.gone()
                } else {
                    expandInputButton.setImageResource(R.drawable.ic_location_feature_expand_more)
                    latitudeInputLayout.visible()
                    longitudeInputLayout.visible()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        markerStateFlow.value = null
    }

    private fun render(state: LocationPositionState) {
        Log.e("location render", state.toString())
        when (state.error) {
            is LocationPositionState.ErrorState.PlaceLatLng -> {
            }
            LocationPositionState.ErrorState.Geocoder -> {
            }
            is LocationPositionState.ErrorState.Input -> with(state.error) {
                if (latitude != null && (latitude < -90.0 || latitude > 90.0))
                    setErrorInput(inputView.latitudeInputLayout, 90)
                else
                    inputView.latitudeInputLayout.error = null
                if (longitude == null || longitude < -180.0 || longitude > 180.0)
                    setErrorInput(inputView.longitudeInputLayout, 180)
                else
                    inputView.longitudeInputLayout.error = null
            }
        }
        _submitState.value = state.nextStepIsAvailable
        if (state.data?.name == null) inputView.placeSearchText.text = ""
        if (state.data == null) return
        if (state.data.name != null) inputView.placeSearchText.text = state.data.name

        inputView.latitudeInputLayout.apply {
            error = null
            editText?.setText(state.data.latLng.latitude.toString())
        }
        inputView.longitudeInputLayout.apply {
            error = null
            editText?.setText(state.data.latLng.longitude.toString())
        }
        if (state.updateMapAfterChangeLocation) {
            Log.e("marker", state.data.latLng.toString())
            markerStateFlow.value = state.data.latLng to state.data.cameraPosition
//            markerChannel.offer(state.subData.latLng to state.subData.cameraPosition)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.setOnMapClickListener { latLng ->
            lifecycleScope.launch {
                googleMap?.run {
                    model.nextLatestAction(
                        LocationPositionAction.ChangeFromMap(
                            latLng,
                            this.cameraPosition
                        )
                    )
                }
            }
            googleMap?.run {
                animateCamera(CameraUpdateFactory.newLatLng(latLng))
                clear()
                addMarker(MarkerOptions().position(latLng))
            }
        }


        markerStateFlow
            .filterNotNull()
            .onEach { (latLng, position) ->
                Log.e("marker each", latLng.toString())
                googleMap?.run {
                    clear()
                    addMarker(MarkerOptions().position(latLng))
                    moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                .apply {
                                    position?.run {
                                        bearing(bearing)
                                        bearing(bearing)
                                        zoom(zoom)
                                        tilt(tilt)
                                    } ?: zoom(10f)
                                }
                                .target(latLng)
                                .build()
                        ))
                }
            }
            .launchIn(lifecycleScope)
    }


    private fun actionFromInput() {
        val lat = inputView.latitudeInputLayout.editText?.text.toString().toDoubleOrNull()
        val lng = inputView.longitudeInputLayout.editText?.text.toString().toDoubleOrNull()
        val latLng = if (lat != null && lng != null) LatLng(lat, lng) else null
        model.nextLatestAction(
            LocationPositionAction.ChangeFromInput(
                lat,
                lng,
                getCameraPosition(latLng)
            )
        )
    }

    private fun setErrorInput(textInputLayout: TextInputLayout, maxRange: Int) {
        textInputLayout.apply {
            error = null
            error = String.format(config.errorFormatString, -maxRange, maxRange)
        }
    }

    private fun getCameraPosition(latLng: LatLng?) = runCatching {
        googleMap?.run {
            cameraPosition?.run {
                CameraPosition
                    .builder(this)
                    .apply { latLng?.run(::target) }
                    .zoom(if (zoom < 10f) 10f; else zoom)
                    .build()
            }
        } ?: CameraPosition.builder()
            .zoom(10f)
            .apply { latLng?.run(::target) }
            .build()
    }.getOrNull()

    fun createDefaultEditPositionView(view: View) =
        EditPositionView(
            view.findViewById(R.id.location_position_place_text),
            view.findViewById(R.id.location_position_latitude_input_layout),
            view.findViewById(R.id.location_position_latitude_edit_text),
            view.findViewById(R.id.location_position_longitude_input_layout),
            view.findViewById(R.id.location_position_longitude_edit_text),
            view.findViewById(R.id.location_position_more_button)
        )

    data class EditPositionView(
        val placeSearchText: TextView,
        val latitudeInputLayout: TextInputLayout,
        val latitudeEditText: TextInputEditText,
        val longitudeInputLayout: TextInputLayout,
        val longitudeEditText: TextInputEditText,
        val expandInputButton: ImageButton
    )

}