package net.arwix.location.demo


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_location_list.*
import net.arwix.location.export.LocationListFeature

class LocationListFragment : Fragment() {

    private lateinit var locationListFeature: LocationListFeature
    private lateinit var resultSetup: LocationListFeature.ResultSetup

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationListFeature = LocationListFeature()
        resultSetup = locationListFeature.setup(
            LocationListFeature.Config(
                modelStoreOwner = this,
                lifecycleOwner = this,
                locationMainFactory = (requireContext().applicationContext as AppApplication).getLocationListFactory()
            ), this
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(location_main_list) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@LocationListFragment.resultSetup.adapter
        }
        lifecycle.addObserver(locationListFeature)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationListFeature.doActivityResult(requestCode, resultCode)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationListFeature.doRequestPermissionsResult(requestCode, grantResults)
    }
}
