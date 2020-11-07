package net.arwix.location.demo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_location_zone.*
import kotlinx.android.synthetic.main.merge_location_prev_next_bar.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.arwix.location.export.LocationZoneFragment


class LocationZoneFragment : LocationZoneFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setup(
            Config(
                modelStoreOwner = this,
                locationZoneFactory = (requireContext().applicationContext as AppApplication).getLocationZoneFactory()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_zone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(location_time_zone_list) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@LocationZoneFragment.getAdapter()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            submitState.collect {
                location_next_button.isEnabled = it
            }
        }
        location_previous_button.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        location_next_button.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                doSubmitLocation()
                requireActivity().supportFragmentManager.popBackStack(
                    null,
                    POP_BACK_STACK_INCLUSIVE
                )
            }
        }
    }

}
