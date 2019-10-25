package net.arwix.location.domain

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object LocationPermissionHelper {


    //  fun isPermission()

    private const val REQUEST_CODE: Int = 592

    fun check(context: Context): Boolean {
        val permissionCheck =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    fun isRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun requestPermissionRationale(
        fragment: Fragment,
        requestCode: Int = REQUEST_CODE,
        force: Boolean = false
    ): Boolean =
        if (!check(fragment.requireContext()) && (isRationale(fragment.requireActivity()) || force)) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
            true
        } else false

    fun requestPermissionRationale(
        activity: Activity,
        requestCode: Int = REQUEST_CODE,
        force: Boolean = false
    ): Boolean =
        if (!check(activity) && (isRationale(activity) || force)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
            true
        } else false


    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        locationRequestCode: Int = REQUEST_CODE
    ) =
        if (requestCode == locationRequestCode) {
            (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        } else false


}