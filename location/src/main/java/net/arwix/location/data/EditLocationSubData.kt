package net.arwix.location.data

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

data class EditLocationSubData(
    val name: String?,
    val subName: String?,
    val latLng: LatLng,
    val cameraPosition: CameraPosition? = null
)