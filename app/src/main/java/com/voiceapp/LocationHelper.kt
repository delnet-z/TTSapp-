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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * 定位帮助类 - GPS/网络/IP 三重定位
 */
class LocationHelper(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * 获取当前位置（GPS/网络）
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

        // 先尝试获取最后一次已知位置（30分钟内有效）
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 30 * 60 * 1000) {
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

        // 协程取消时清理 listener，防止内存泄漏
        cont.invokeOnCancellation {
            if (!resumed) {
                resumed = true
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
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

    /**
     * IP 定位兜底 - 免费无需 Key，精度到城市级别
     */
    suspend fun getIpLocation(): Result<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url("http://ip-api.com/json/").get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("IP定位失败: ${response.code}"))
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("IP定位返回空"))
            val json = JSONObject(body)
            val lat = json.getDouble("lat")
            val lon = json.getDouble("lon")
            Result.success(Pair(lat, lon))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
