package net.arwix.location.demo


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_location_list.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.export.LibLocationListFragment
import java.lang.ref.WeakReference

class LocationListFragment : LibLocationListFragment() {

//    private lateinit var locationListFeature: LocationListFeature

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        locationListFeature = LocationListFeature()
        setup(
            Config(
                modelStoreOwner = this,
                locationMainFactory = (requireContext().applicationContext as AppApplication).getLocationListFactory(),
                colorOnSecondary = 0xFF000000.toInt(),
                gotoEditFragment = {
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
//        lifecycle.addObserver(locationListFeature)
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
            adapter = this@LocationListFragment.getAdapter()
        }
        location_add_button.setOnClickListener {
            doAddLocation()
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
            lifecycleScope.launch {
                commitSelectedItem()
            }
        }
        lifecycleScope.launch {
            submitState.collect {
                refButton.get()?.run {
                    isEnabled = it
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        doActivityResult(requestCode, resultCode)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        doRequestPermissionsResult(requestCode, grantResults)
    }
}
