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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.export.LocationListFeature
import java.lang.ref.WeakReference

class LocationListFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var locationListFeature: LocationListFeature
    private lateinit var result: LocationListFeature.Result

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationListFeature = LocationListFeature()
        result = locationListFeature.setup(
            LocationListFeature.Config(
                modelStoreOwner = this,
                lifecycleOwner = this,
                locationMainFactory = (requireContext().applicationContext as AppApplication).getLocationListFactory(),
                onSecondaryColor = 0xFF000000.toInt(),
                onEditView = {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container,
                            LocationPositionFragment::class.java, null
                        )
                        .addToBackStack(null)
                        .commit()
                }
            ), this
        )
        lifecycle.addObserver(locationListFeature)
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
            adapter = this@LocationListFragment.result.adapter
        }
        location_add_button.setOnClickListener {
            locationListFeature.doAddLocation()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    LocationPositionFragment::class.java, null
                )
                .addToBackStack(null)
                .commit()
        }
        val refButton = WeakReference(location_main_submit_button)
        location_main_submit_button.setOnClickListener {
            launch {
                locationListFeature.commitSelectedItem()
            }
        }
        launch {
            result.flowSubmitAvailable.collect {
                refButton.get()?.run {
                    isEnabled = it
                }
            }
        }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationListFeature.doRequestPermissionsResult(requestCode, grantResults)
    }
}
