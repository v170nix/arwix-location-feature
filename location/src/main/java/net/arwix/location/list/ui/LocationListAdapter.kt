package net.arwix.location.list.ui

//import org.threeten.bp.Instant
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.arwix.extension.gone
import net.arwix.extension.visible
import net.arwix.location.R
import net.arwix.location.common.extension.latToString
import net.arwix.location.common.extension.lngToString
import net.arwix.location.data.gtimezone.TimeZoneRepository
import net.arwix.location.data.room.LocationTimeZoneData
import java.time.Instant

class LocationListAdapter(
    private val instant: Instant,
    private val nsweStrings: Array<String> = arrayOf("N", "S", "W", "E"),
    @ColorInt private val onSecondaryColor: Int,
    private val onRequestPermission: () -> Unit,
    private val onUpdateAutoLocation: () -> Unit,
    private val onSelectedListener: ((item: LocationTimeZoneData, isAuto: Boolean) -> Unit)? = null,
    private val onEditListener: ((item: LocationTimeZoneData) -> Unit)? = null,
    private val onDeleteListener: ((item: LocationTimeZoneData) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val differ = AsyncListDiffer(this, diffCallback)

    init {
        AsyncListDiffer(this, diffCallback)
        // fix detached view should be removed from RecyclerView before it can be recycled
        setHasStableIds(false)
    }

    private val onEditClickListener = View.OnClickListener { v ->
        val item = v.tag as Item.Manual
        onEditListener?.invoke(item.data)
    }

    private val onDeleteClickListener = View.OnClickListener { v ->
        val item = v.tag as Item.Manual
        onDeleteListener?.invoke(item.data)
    }

    private val onSelectedClickListener = View.OnClickListener { v ->
        val item = v.tag as Item
        if (item is Item.Auto && item.state is AutoState.Allow && item.state.data != null) {
            if (item.state.data.isSelected) return@OnClickListener
            onSelectedListener?.invoke(item.state.data, true)
        }
        if (item is Item.Manual) {
            if (item.data.isSelected) return@OnClickListener
            onSelectedListener?.invoke(item.data, false)
        }
    }

    fun setData(newData: List<LocationTimeZoneData>, autoState: AutoState) {
        val autoItem = newData.find { it.isAutoLocation }
        val customData = newData.filter { !it.isAutoLocation }
        val data = mutableListOf<Item>().apply {
            if (autoState is AutoState.Allow && autoItem != null) {
                add(Item.Auto(autoState.copy(autoItem)))
            } else {
                add(Item.Auto(autoState))
            }
            addAll(customData.map { Item.Manual(it) }.asReversed())
        }
        if (data == differ.currentList) return
        differ.submitList(data)
//        if (data == items) return
//        val diffCallback = ItemDiffCallback(items, data)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//        items.clear()
//        items.addAll(data)
//        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            1 -> AutoViewHolder(
                inflater.inflate(R.layout.item_location_list_auto, parent, false),
                instant,
                nsweStrings,
                onSecondaryColor
            )
            2 -> ManualViewHolder(
                inflater.inflate(R.layout.item_location_list_manual, parent, false),
                instant,
                nsweStrings,
                onSecondaryColor
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = differ.currentList[position]  // items[position]
        if (item is Item.Manual) {
            holder as CustomLocationViewHolder
            with(holder) {
                setTag(item)
                if (item.data.isSelected) holder.selected()
                else holder.deselected()
                setListeners(onSelectedClickListener, onEditClickListener, onDeleteClickListener)
                setState(item)
            }
        }
        if (item is Item.Auto) {
            holder as AutoLocationViewHolder
            with(holder) {
                setTag(item)
                if (item.state is AutoState.Allow && item.state.data?.isSelected == true) holder.selected()
                else holder.deselected()
                setListeners(onSelectedClickListener, onUpdateAutoLocation, onRequestPermission)
                setState(item.state)
            }
        }
    }

    override fun getItemCount() = differ.currentList.size // items.size

    override fun getItemViewType(position: Int) =
        differ.currentList[position].viewType // items[position].viewType

    interface Select {
        fun selected()
        fun deselected()
    }

    abstract class AutoLocationViewHolder(view: View) : Select, RecyclerView.ViewHolder(view) {
        abstract fun setTag(any: Any)
        abstract fun setListeners(
            selectedListener: View.OnClickListener,
            updateAutoLocation: () -> Unit,
            requestPermission: () -> Unit
        )

        abstract fun setState(item: AutoState)
    }

    abstract class CustomLocationViewHolder(view: View) : Select, RecyclerView.ViewHolder(view) {
        abstract fun setTag(any: Any)
        abstract fun setListeners(
            selectedListener: View.OnClickListener,
            editListener: View.OnClickListener,
            deleteListener: View.OnClickListener
        )
        abstract fun setState(item: Item.Manual)
    }

    private class AutoViewHolder(
        view: View,
        private val instant: Instant,
        private val nsweStrings: Array<String> = arrayOf("N", "S", "W", "E"),
        private val onSecondaryColor: Int
    ) : AutoLocationViewHolder(view) {
        private val permissionDeniedInfo: TextView =
            view.findViewById(R.id.location_permission_denied_info)
        val permissionDeniedButton: Button =
            view.findViewById(R.id.location_permission_rationale_button)
        private val successLayout =
            view.findViewById<View>(R.id.location_main_item_auto_success_layout)
        private val successNoneLayout =
            view.findViewById<View>(R.id.location_main_item_auto_none_layout)
        private val headerView: TextView = view.findViewById(R.id.location_main_auto_header)
        private val nameView: TextView = view.findViewById(R.id.location_main_item_auto_name_text)
        private val subNameView: TextView =
            view.findViewById(R.id.location_main_item_auto_sub_name_text)
        private val latitudeView: TextView =
            view.findViewById(R.id.location_main_item_auto_latitude_text)
        private val longitudeView: TextView =
            view.findViewById(R.id.location_main_item_auto_longitude_text)
        private val zoneNameView: TextView =
            view.findViewById(R.id.location_main_item_auto_time_zone_first)
        private val zoneSubNameView: TextView =
            view.findViewById(R.id.location_main_item_auto_time_zone_second)
        private val zoneGmtOffsetView: TextView =
            view.findViewById(R.id.location_main_item_auto_time_zone_three)
        val updateButton: Button =
            view.findViewById(R.id.location_main_item_auto_success_update_button)
        val updateLocationButton: Button =
            view.findViewById(R.id.location_main_item_auto_update_button)
        val layout: ConstraintLayout = view.findViewById(R.id.location_main_item_auto_layout)
        private val defaultTextColors = latitudeView.textColors
        private val headerTextColors = headerView.textColors

        override fun setListeners(
            selectedListener: View.OnClickListener,
            updateAutoLocation: () -> Unit,
            requestPermission: () -> Unit
        ) {
            layout.setOnClickListener(selectedListener)
            updateButton.setOnClickListener {
                updateAutoLocation()
            }
            updateLocationButton.setOnClickListener {
                updateAutoLocation()
            }
            permissionDeniedButton.setOnClickListener {
                requestPermission()
            }
        }

        override fun setTag(any: Any) {
            layout.tag = any
        }

        override fun setState(item: AutoState) {
            when (item) {
                AutoState.None -> viewNone()
                AutoState.Denied -> viewDenied()
                AutoState.DeniedRationale -> viewDeniedRationale()
                is AutoState.Allow -> viewAllow(item.data)
            }
        }

        override fun selected() {
            layout.isSelected = true
            headerView.setTextColor(onSecondaryColor)
            nameView.setTextColor(onSecondaryColor)
            subNameView.setTextColor(onSecondaryColor)
            latitudeView.setTextColor(onSecondaryColor)
            longitudeView.setTextColor(onSecondaryColor)
            zoneNameView.setTextColor(onSecondaryColor)
            zoneSubNameView.setTextColor(onSecondaryColor)
            zoneGmtOffsetView.setTextColor(onSecondaryColor)
            latitudeView.setTextColor(onSecondaryColor)
        }

        override fun deselected() {
            layout.isSelected = false
            headerView.setTextColor(headerTextColors)
            nameView.setTextColor(defaultTextColors)
            subNameView.setTextColor(defaultTextColors)
            latitudeView.setTextColor(defaultTextColors)
            longitudeView.setTextColor(defaultTextColors)
            zoneNameView.setTextColor(defaultTextColors)
            zoneSubNameView.setTextColor(defaultTextColors)
            zoneGmtOffsetView.setTextColor(defaultTextColors)
            latitudeView.setTextColor(defaultTextColors)
        }

        private fun viewDenied() {
            permissionDeniedInfo.visible()
            successLayout.gone()
            successNoneLayout.gone()
            layout.background = null
            //    permissionDeniedButton.gone()
        }

        private fun viewDeniedRationale() {
            successLayout.gone()
            successNoneLayout.gone()
            permissionDeniedInfo.visible()
            permissionDeniedButton.visible()
            layout.background = null
        }

        private fun viewAllow(data: LocationTimeZoneData?) {
            permissionDeniedInfo.gone()
            permissionDeniedButton.gone()
            if (data == null) {
                successLayout.gone()
                successNoneLayout.visible()
                layout.background = null
                return
            }
            layout.setBackgroundResource(R.drawable.location_selected_list_item)
            successLayout.visible()
            successNoneLayout.gone()
            latitudeView.text = latToString(data.latLng.latitude, nsweStrings[0], nsweStrings[1])
            longitudeView.text = lngToString(data.latLng.longitude, nsweStrings[2], nsweStrings[3])
            nameView.text = data.name ?: ""
            subNameView.text = data.subName ?: ""
            zoneNameView.text = TimeZoneRepository.getName(data.zone)
            zoneSubNameView.text = TimeZoneRepository.getLongName(data.zone, instant)
            zoneGmtOffsetView.text =
                TimeZoneRepository.getGmtOffsetText(data.zone, instant)

        }

        private fun viewNone() {
            successLayout.gone()
            permissionDeniedInfo.gone()
            permissionDeniedButton.gone()
        }
    }

    private class ManualViewHolder(
        view: View,
        private val instant: Instant,
        private val nsweStrings: Array<String> = arrayOf("N", "S", "W", "E"),
        private val onSecondaryColor: Int
    ) : CustomLocationViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.location_main_item_name_text)
        private val subNameView: TextView = view.findViewById(R.id.location_main_item_sub_name_text)
        private val latitudeView: TextView =
            view.findViewById(R.id.location_main_item_latitude_text)
        private val longitudeView: TextView =
            view.findViewById(R.id.location_main_item_longitude_text)
        private val zoneNameView: TextView =
            view.findViewById(R.id.location_main_item_time_zone_first)
        private val zoneSubNameView: TextView =
            view.findViewById(R.id.location_main_item_time_zone_second)
        private val zoneGmtOffsetView: TextView =
            view.findViewById(R.id.location_main_item_time_zone_three)
        private val editButton: Button = view.findViewById(R.id.location_main_item_edit_button)
        private val deleteButton: Button = view.findViewById(R.id.location_main_item_delete_button)
        private val layout: View = view.findViewById(R.id.location_main_item_layout)
        private val defaultTextColors = latitudeView.textColors

        init {
            layout.setBackgroundResource(R.drawable.location_selected_list_item)
        }

        override fun setTag(any: Any) {
            layout.tag = any
            editButton.tag = any
            deleteButton.tag = any
        }

        override fun setListeners(
            selectedListener: View.OnClickListener,
            editListener: View.OnClickListener,
            deleteListener: View.OnClickListener
        ) {
            layout.setOnClickListener(selectedListener)
            editButton.setOnClickListener(editListener)
            deleteButton.setOnClickListener(deleteListener)
        }

        override fun setState(item: Item.Manual) {
            latitudeView.text =
                latToString(item.data.latLng.latitude, nsweStrings[0], nsweStrings[1])
            longitudeView.text =
                lngToString(item.data.latLng.longitude, nsweStrings[2], nsweStrings[3])
            nameView.apply {
                text = item.data.name
                if (item.data.name.isNullOrEmpty()) gone() else visible()
            }
            subNameView.apply {
                text = item.data.subName
                if (item.data.subName.isNullOrEmpty()) gone() else visible()
            }
            zoneNameView.text = TimeZoneRepository.getName(item.data.zone)
            zoneSubNameView.text = TimeZoneRepository.getLongName(item.data.zone, instant)
            zoneGmtOffsetView.text =
                TimeZoneRepository.getGmtOffsetText(item.data.zone, instant)
            layout.tag = item
        }

        override fun selected() {
            layout.isSelected = true
            nameView.setTextColor(onSecondaryColor)
            subNameView.setTextColor(onSecondaryColor)
            latitudeView.setTextColor(onSecondaryColor)
            longitudeView.setTextColor(onSecondaryColor)
            zoneNameView.setTextColor(onSecondaryColor)
            zoneSubNameView.setTextColor(onSecondaryColor)
            zoneGmtOffsetView.setTextColor(onSecondaryColor)
            latitudeView.setTextColor(onSecondaryColor)
        }

        override fun deselected() {
            layout.isSelected = false
            nameView.setTextColor(defaultTextColors)
            subNameView.setTextColor(defaultTextColors)
            latitudeView.setTextColor(defaultTextColors)
            longitudeView.setTextColor(defaultTextColors)
            zoneNameView.setTextColor(defaultTextColors)
            zoneSubNameView.setTextColor(defaultTextColors)
            zoneGmtOffsetView.setTextColor(defaultTextColors)
            latitudeView.setTextColor(defaultTextColors)
        }
    }

    private companion object {
        private val diffCallback by lazy(LazyThreadSafetyMode.NONE) {
            object : DiffUtil.ItemCallback<Item>() {

                override fun areItemsTheSame(
                    oldItem: Item,
                    newItem: Item
                ): Boolean {
                    return ((oldItem is Item.Auto && newItem is Item.Auto) ||
                            ((oldItem is Item.Manual && newItem is Item.Manual) &&
                                    oldItem.data.id == newItem.data.id))
                }

                override fun areContentsTheSame(
                    oldItem: Item,
                    newItem: Item
                ): Boolean {
                    if (oldItem is Item.Auto && newItem is Item.Auto) {
                        return (oldItem.state == newItem.state)
                    }
                    if (oldItem is Item.Auto || newItem is Item.Auto) return false
                    oldItem as Item.Manual
                    newItem as Item.Manual
                    return (
                            oldItem.data.zone.id == newItem.data.zone.id
                                    && oldItem.data.latLng == newItem.data.latLng
                                    && oldItem.data.name == newItem.data.name
                                    && oldItem.data.subName == newItem.data.subName
                                    && oldItem.data.isSelected == newItem.data.isSelected
                            )
                }
            }
        }
    }

    sealed class AutoState {
        object None : AutoState()
        object Denied : AutoState()
        object DeniedRationale : AutoState()
        data class Allow(val data: LocationTimeZoneData?) : AutoState()
    }

    sealed class Item(val viewType: Int) {
        data class Auto(val state: AutoState) : Item(1)
        data class Manual(val data: LocationTimeZoneData) : Item(2)
    }

}