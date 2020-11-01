package net.arwix.location.demo


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_location_list.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.export.LocationListFragment
import java.lang.ref.WeakReference

class LocationListFragment : LocationListFragment() {

//    private lateinit var locationListFeature: LocationListFeature

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setup(
            Config(
                modelStoreOwner = this,
                locationMainFactory = (requireContext().applicationContext as AppApplication).getLocationListFactory(),
                colorOnSecondary = 0xFF000000.toInt()
            )
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
//        location_main_submit_button.setOnClickListener {
//            lifecycleScope.launch {
//                commitSelectedItem()
//            }
//        }
        lifecycleScope.launch {
            submitState.collect {
                refButton.get()?.run {
                    isEnabled = it
                }
            }
        }
    }

    override fun navigateToEditItemFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                LocationPositionFragment::class.java, null
            )
            .addToBackStack(null)
            .commit()
    }
}
