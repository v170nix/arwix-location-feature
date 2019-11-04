package net.arwix.location.demo


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.merge_location_prev_next_bar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.export.LocationPositionFeature

class LocationPositionFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var positionFeature: LocationPositionFeature
    private lateinit var result: LocationPositionFeature.Result

    override fun onAttach(context: Context) {
        super.onAttach(context)
        positionFeature = LocationPositionFeature()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_position, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        result = positionFeature.setup(
            LocationPositionFeature.Config(
                modelStoreOwner = this,
                lifecycleOwner = this,
                locationPositionFactory = (requireContext().applicationContext as AppApplication).getLocationPositionFactory(),
                mapFragment = childFragmentManager.findFragmentById(R.id.location_position_map) as SupportMapFragment,
                placeKey = "AIzaSyAvar6k6vq66mfSMOsttrw_09gqNSWae3g",
                inputView = positionFeature.createDefaultEditPositionView(view)
            ), this
        )
        lifecycle.addObserver(positionFeature)
        launch {
            result.flowNextStepAvailable.collect {
                location_next_button.isEnabled = it
            }
        }
        location_next_button.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    LocationZoneFragment::class.java, null
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        positionFeature.doActivityResult(requestCode, resultCode, data)
    }
}
