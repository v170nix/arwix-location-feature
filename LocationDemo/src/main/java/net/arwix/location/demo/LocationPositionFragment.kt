package net.arwix.location.demo


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.fragment_location_position.*
import kotlinx.android.synthetic.main.merge_location_prev_next_bar.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.export.LocationPositionFragment

class LocationPositionFragment : LocationPositionFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_position, container, false)
            .also { inputView = createDefaultEditPositionView(it) }
    }

    override lateinit var inputView: EditPositionView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup(
            Config(
                modelStoreOwner = this,
                locationPositionFactory = (requireContext().applicationContext as AppApplication).getLocationPositionFactory(),
                mapFragment = childFragmentManager.findFragmentById(R.id.location_position_map) as SupportMapFragment,
                placeKey = "AIzaSyAvar6k6vq66mfSMOsttrw_09gqNSWae3g",
            )
        )
//        positionFragment.setup(
//            LocationPositionFragment.Config(
//                modelStoreOwner = this,
//                lifecycleOwner = this,
//                locationPositionFactory = (requireContext().applicationContext as AppApplication).getLocationPositionFactory(),
//                mapFragment = childFragmentManager.findFragmentById(R.id.location_position_map) as SupportMapFragment,
//                placeKey = "AIzaSyAvar6k6vq66mfSMOsttrw_09gqNSWae3g",
//                inputView = positionFragment.createDefaultEditPositionView(view)
//            ), this
//        )
//        lifecycle.addObserver(positionFragment)
        lifecycleScope.launch {
            submitState.collect {
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
        location_position_input_layout.setBackgroundColor(resources.getColor(R.color.colorBgOnMap))
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        doActivityResult(requestCode, resultCode, data)
//    }
}
