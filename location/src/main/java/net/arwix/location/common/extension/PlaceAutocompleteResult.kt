package net.arwix.location.common.extension

import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place

sealed class PlaceAutocompleteResult {
    object Canceled : PlaceAutocompleteResult()
    data class Error(val status: Status) : PlaceAutocompleteResult()
    data class Ok(val place: Place) : PlaceAutocompleteResult()

    fun getPlaceInResult(): Place? {
        if (this is Ok) return this.place
        return null
    }
}

