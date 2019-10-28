package net.arwix.location.common.extension

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

sealed class PlaceAutocompleteResult {
    object Canceled : PlaceAutocompleteResult()
    data class Error(val status: Status) : PlaceAutocompleteResult()
    data class Ok(val place: Place) : PlaceAutocompleteResult()

    fun getPlaceInResult(): Place? {
        if (this is Ok) return this.place
        return null
    }
}

const val AUTOCOMPLETE_REQUEST_CODE = 278

fun Fragment.startPlace(
    fields: List<Place.Field> = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.LAT_LNG
    ),
    requestCode: Int = AUTOCOMPLETE_REQUEST_CODE
) {
    val intent =
        Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
    startActivityForResult(
        intent,
        requestCode
    )
}

fun checkPlace(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    placeRequestCode: Int = AUTOCOMPLETE_REQUEST_CODE
): PlaceAutocompleteResult? {
    if (data == null) return null
    if (requestCode == placeRequestCode) {
        return when (resultCode) {
            RESULT_OK -> PlaceAutocompleteResult.Ok(
                Autocomplete.getPlaceFromIntent(
                    data
                )
            )
            AutocompleteActivity.RESULT_ERROR -> PlaceAutocompleteResult.Error(
                Autocomplete.getStatusFromIntent(data)
            )
            RESULT_CANCELED -> PlaceAutocompleteResult.Canceled
            else -> null
        }
    }
    return null
}

