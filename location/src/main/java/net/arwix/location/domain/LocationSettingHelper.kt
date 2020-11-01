package net.arwix.location.domain

import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import net.arwix.location.common.extension.await
import java.lang.ref.WeakReference

object LocationSettingHelper {

    private const val REQUEST_SETTINGS_CODE: Int = 594

    suspend fun check(
        fragment: Fragment,
        requestCode: Int = REQUEST_SETTINGS_CODE
    ): IntentSenderRequest? {
        val client = LocationServices.getSettingsClient(fragment.requireActivity())
        val weakFragment = WeakReference(fragment)
        val request = LocationRequest().apply { priority = LocationRequest.PRIORITY_HIGH_ACCURACY }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(request)
        runCatching { client.checkLocationSettings(builder.build()).await() }
            .onFailure {
                if (it is ApiException && it.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    it as ResolvableApiException
                    //https://stackoverflow.com/questions/31235564/locationsettingsrequest-dialog-to-enable-gps-onactivityresult-skipped/39579124#39579124
                    weakFragment.get()?.run {
                        return IntentSenderRequest.Builder(it.resolution.intentSender).build()
                    }
                }
            }
        return null
    }

}