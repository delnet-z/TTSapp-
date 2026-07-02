package com.voiceapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 定位帮助类 - 获取GPS/网络位置
 */
class LocationHelper(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * 获取当前位置，超时10秒
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Location> = suspendCancellableCoroutine { cont ->
        val hasPermission = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            cont.resume(Result.failure(Exception("定位权限未授予")))
            return@suspendCancellableCoroutine
        }

        // 先尝试获取最后一次已知位置
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 5 * 60 * 1000) {
            cont.resume(Result.success(lastKnown))
            return@suspendCancellableCoroutine
        }

        // 请求单次位置更新
        var resumed = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!resumed) {
                    resumed = true
                    locationManager.removeUpdates(this)
                    cont.resume(Result.success(location))
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                if (!resumed) {
                    resumed = true
                    locationManager.removeUpdates(this)
                    cont.resume(Result.failure(Exception("GPS已关闭")))
                }
            }
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            try {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
            } catch (e2: Exception) {
                cont.resume(Result.failure(e2))
            }
        }
    }
}
