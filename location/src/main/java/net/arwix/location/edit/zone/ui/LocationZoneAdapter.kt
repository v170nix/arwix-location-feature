package net.arwix.location.edit.zone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_zone.view.*
import kotlinx.android.synthetic.main.item_location_zone_auto.view.*
import kotlinx.coroutines.*
import net.arwix.extension.gone
import net.arwix.extension.invisible
import net.arwix.extension.visible
import net.arwix.extension.weak
import net.arwix.location.R
import net.arwix.location.data.TimeZoneDisplayEntry
import net.arwix.location.data.TimeZoneRepository
//import org.threeten.bp.Instant
//import org.threeten.bp.ZoneId
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.ZoneId

class LocationZoneAdapter(
    private val scope: CoroutineScope,
    private val listener: ((zoneId: ZoneId, isAuto: Boolean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Item>()
    private var selectionId = -1

    private val onClickListener = View.OnClickListener { v ->
        val item = v.tag as Item
        val id = items.indexOf(item)
        if (id != selectionId && item is Item.Manual) {
            listener?.invoke(item.entry.id, false)
        }
    }

    private val onAutoClickListener = View.OnClickListener { v ->
        val item = v.tag as Item
        val id = items.indexOf(item)
        if (id != selectionId && (item is Item.Auto)) {
            val state = autoState
            if (state !is LocationZoneState.AutoZoneStatus.Ok) return@OnClickListener
            listener?.invoke(state.data, true)
        }
    }

    init {
        setHasStableIds(true)
    }

    private var autoState: LocationZoneState.AutoZoneStatus? = null

    fun setAutoState(
        newAutoState: LocationZoneState.AutoZoneStatus,
        isAutomaticSelected: Boolean
    ) {
        if (autoState == newAutoState) return
        autoState = newAutoState
        if (isAutomaticSelected &&
            selectionId == -1 &&
            newAutoState is LocationZoneState.AutoZoneStatus.Ok
        ) {
            listener?.invoke(newAutoState.data, true)
        }
        getAutoIndex()?.let(::notifyItemChanged)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> HeaderViewHolder(
                inflater.inflate(
                    R.layout.item_location_zone_header_auto,
                    parent,
                    false
                )
            )
            1 -> HeaderViewHolder(
                inflater.inflate(
                    R.layout.item_location_zone_header_manual,
                    parent,
                    false
                )
            )
            2 -> AutoViewHolder(inflater.inflate(R.layout.item_location_zone_auto, parent, false))
            3 -> ManualViewHolder(inflater.inflate(R.layout.item_location_zone, parent, false))
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (item is Item.Manual) with(holder as ManualViewHolder) {
            setData(item.entry)
            view.isActivated = selectionId == position
            view.tag = item
            view.setOnClickListener(onClickListener)
        }
        if (item is Item.Auto) with(holder as AutoViewHolder) holder@{
            view.tag = item
            view.setOnClickListener(onAutoClickListener)
            scope.launch {
                autoState?.run {
                    this@holder.doResult(this)
                    this@holder.view.isActivated = selectionId == position
                }
            }
        }

    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int) = items[position].viewType

    override fun getItemId(position: Int) = position.toLong()

    @UiThread
    fun setList(list: List<TimeZoneDisplayEntry>) {
        if (list != items) {
            items.clear()
            items.add(Item.HeaderAuto)
            items.add(Item.Auto)
            items.add(Item.HeaderManual)
            items.addAll(list.map { Item.Manual(it) })
            notifyDataSetChanged()
        }
    }

    private fun deselect() {
        val id = selectionId
        if (id > -1) {
            selectionId = -1
            notifyItemChanged(id)
        }
    }

    fun selectAuto() {
        getAutoIndex()?.let { index ->
            if (selectionId == index) return
            deselect()
            selectionId = index
            notifyItemChanged(index)
        }
    }

    fun select(zoneId: ZoneId) {
        items.forEachIndexed { index, item ->
            if ((item is Item.Manual) && (item.entry.id == zoneId)) {
                deselect()
                selectionId = index
                notifyItemChanged(index)
                return
            }
        }
    }

    private fun getAutoIndex() = if (items.size > 0) {
        items
            .indexOfFirst { it is Item.Auto }
            .takeIf { it > -1 }
    } else null

    private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private class ManualViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val firstView: TextView = view.location_item_time_zone_first_text
        private val secondaryView: TextView = view.location_item_time_zone_secondary_text
        private val threeView: TextView = view.location_item_time_zone_three_text

        init {
            view.setBackgroundResource(R.drawable.location_selected_list_item)
        }

        fun setData(item: TimeZoneDisplayEntry) {
            firstView.text = item.displayName
            secondaryView.text = item.displayLongName
            threeView.text = item.gmtOffsetString
        }
    }

    private class AutoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val resultLayout: ViewGroup = view.location_time_zone_item_auto_layout
        private val loadingProgressBar: ProgressBar = view.location_time_zone_item_auto_progress
        private val resultFirstText: TextView = view.location_time_zone_item_auto_first_text
        private val resultSecondaryText: TextView = view.location_time_zone_item_auto_secondary_text
        private val resultThreeText: TextView = view.location_time_zone_item_auto_three_text
        private val resultErrorText: TextView = view.location_time_zone_item_auto_error_text

        var currentStatus: LocationZoneState.AutoZoneStatus? = null
        private val weakResultLayout: WeakReference<ViewGroup> = resultLayout.weak()

        init {
            beginLoading()
            loadingProgressBar.gone()
        }

        suspend fun doResult(result: LocationZoneState.AutoZoneStatus) =
            withContext(Dispatchers.Main) {
                if (currentStatus == result) return@withContext
                if (result !is LocationZoneState.AutoZoneStatus.Ok) deselect()
                consumeAction(result)
                currentStatus = result
            }

        fun deselect() {
            view.isActivated = false
        }

        fun select() {
            view.isActivated = true
        }

        private suspend fun consumeAction(status: LocationZoneState.AutoZoneStatus) {
            when (status) {
                is LocationZoneState.AutoZoneStatus.Loading -> {
                    resultLayout.background = null
                    beginLoading()
                }
                else -> {
                    if (currentStatus is LocationZoneState.AutoZoneStatus.Loading) {
                        delay(300L)
                    }
                    if (currentStatus != null) {
                        loadingProgressBar.animate()
                            .alpha(0f)
                            .withEndAction {
                                loadingProgressBar.gone()
                                startResultAnimation()
                            }
                            .setDuration(100L)
                            .start()
                    } else {
                        loadingProgressBar.gone()
                        resultLayout.visible()
                        resultLayout.alpha = 1f
                    }

                    if (status is LocationZoneState.AutoZoneStatus.Ok) {
                        beginViewOk()
                        resultLayout.setBackgroundResource(R.drawable.location_selected_list_item)
                        val instant = Instant.now()
                        resultFirstText.text = TimeZoneRepository.getName(status.data)
                        resultSecondaryText.text =
                            TimeZoneRepository.getLongName(status.data, instant)
                        resultThreeText.text =
                            TimeZoneRepository.getGmtOffsetText(status.data, instant)
                    }
                    if (status is LocationZoneState.AutoZoneStatus.Error) {
                        beginViewError()
                        resultLayout.background = null
                    }
                }
            }
        }

        private fun beginLoading() {
            resultLayout.alpha = 0f
            resultLayout.invisible()
            val layoutParams = resultLayout.layoutParams
            layoutParams.height = (resultLayout.resources.displayMetrics.density * 88).toInt()
            resultLayout.layoutParams = layoutParams
            resultErrorText.gone()
            resultFirstText.gone()
            resultSecondaryText.gone()
            resultThreeText.gone()
            loadingProgressBar.visible().alpha = 1f
        }

        private fun beginViewOk() {
            resultErrorText.gone()
            resultFirstText.visible()
            resultSecondaryText.visible()
            resultThreeText.visible()
        }

        private fun beginViewError() {
            resultErrorText.visible()
            resultFirstText.gone()
            resultSecondaryText.gone()
            resultThreeText.gone()
            val layoutParams = resultLayout.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            resultLayout.layoutParams = layoutParams
        }


        private fun startResultAnimation() {
            resultLayout
                .animate()
                .withStartAction {
                    resultLayout.alpha = 0f
                    resultLayout.visible()
                }
                .alpha(1f)
                .setDuration(100L)
                .start()
        }
    }

    private sealed class Item(val viewType: Int) {
        object HeaderAuto : Item(0)
        object HeaderManual : Item(1)
        object Auto : Item(2)
        data class Manual(val entry: TimeZoneDisplayEntry) : Item(3)
    }
}