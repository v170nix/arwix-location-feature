package net.arwix.location.export

import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.merge_location_position_input_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.arwix.extension.*
import net.arwix.location.R
import net.arwix.location.common.extension.checkPlace
import net.arwix.location.common.extension.startPlace
import net.arwix.location.ui.position.LocationPositionAction
import net.arwix.location.ui.position.LocationPositionState
import net.arwix.location.ui.position.LocationPositionViewModel

class LocationPositionFeature : LifecycleObserver, CoroutineScope by MainScope(),
    OnMapReadyCallback {

    @Suppress("ArrayInDataClass")
    data class Config(
        val modelStoreOwner: ViewModelStoreOwner,
        val lifecycleOwner: LifecycleOwner,
        val locationPositionFactory: ViewModelProvider.Factory,
        val mapFragment: SupportMapFragment,
        val placeKey: String,
        val inputView: EditPositionView,
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

    private val _submitState = MutableStateFlow(false)
    val submitState: StateFlow<Boolean> = _submitState

    fun setup(config: Config, fragment: Fragment) {
        this.config = config
        if (!Places.isInitialized()) {
            Places.initialize(fragment.requireContext().applicationContext, config.placeKey)
        }
        config.inputView.apply {
            val weakFragment = fragment.weak()
            placeSearchText.setOnClickListener {
                weakFragment.runWeak { startPlace(placeFields) }
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
                if (this@LocationPositionFeature.config.inputView.latitudeInputLayout.visibility == View.VISIBLE) {
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

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        model = ViewModelProvider(
            config.modelStoreOwner,
            config.locationPositionFactory
        ).get(LocationPositionViewModel::class.java)
        model.state.observe(config.lifecycleOwner, Observer(::render))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        googleMap = null
        cancel()
    }

    private fun render(state: LocationPositionState) {
        when (state.error) {
            is LocationPositionState.ErrorState.PlaceLatLng -> {
            }
            LocationPositionState.ErrorState.Geocoder -> {
            }
            is LocationPositionState.ErrorState.Input -> with(state.error) {
                if (latitude != null && (latitude < -90.0 || latitude > 90.0))
                    setErrorInput(config.inputView.latitudeInputLayout, 90)
                else
                    config.inputView.latitudeInputLayout.error = null
                if (longitude == null || longitude < -180.0 || longitude > 180.0)
                    setErrorInput(config.inputView.longitudeInputLayout, 180)
                else
                    config.inputView.longitudeInputLayout.error = null
            }
        }
        _submitState.value = state.nextStepAvailable
        if (state.subData?.name == null) config.inputView.placeSearchText.text = ""
        if (state.subData == null) return
        if (state.subData.name != null) config.inputView.placeSearchText.text = state.subData.name

        config.inputView.latitudeInputLayout.apply {
            error = null
            editText?.setText(state.subData.latLng.latitude.toString())
        }
        config.inputView.longitudeInputLayout.apply {
            error = null
            editText?.setText(state.subData.latLng.longitude.toString())
        }
        if (state.updateMapAfterChangeLocation) {
            markerStateFlow.value = state.subData.latLng to state.subData.cameraPosition
//            markerChannel.offer(state.subData.latLng to state.subData.cameraPosition)
        }
    }

    fun doActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        checkPlace(requestCode, resultCode, data)
            ?.getPlaceInResult()
            ?.run {
                model.nextLatestAction(
                    LocationPositionAction.ChangeFromPlace(this, getCameraPosition(latLng))
                )
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.setOnMapClickListener { latLng ->
            launch {
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
            .launchIn(this)
    }

    private fun actionFromInput() {
        val lat = config.inputView.latitudeInputLayout.editText?.text.toString().toDoubleOrNull()
        val lng = config.inputView.longitudeInputLayout.editText?.text.toString().toDoubleOrNull()
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
            view.location_position_place_text,
            view.location_position_latitude_input_layout,
            view.location_position_latitude_edit_text,
            view.location_position_longitude_input_layout,
            view.location_position_longitude_edit_text,
            view.location_position_more_button
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