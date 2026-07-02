package com.voiceapp

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo 天气数据模型（免费，无需API Key）
 */
data class WeatherData(
    val temperature: Double,       // 当前温度 摄氏度
    val temperatureMax: Double,    // 今日最高温 摄氏度
    val temperatureMin: Double,    // 今日最低温 摄氏度
    val humidity: Int,             // 湿度百分比
    val precipitationProb: Int,    // 降水概率百分比
    val weatherCode: Int,          // WMO天气代码
    val windSpeed: Double,         // 风速 km/h
    val description: String        // 中文天气描述
) {
    companion object {
        // WMO Weather interpretation codes
        private val weatherMap = mapOf(
            0 to "晴天", 1 to "大部晴朗", 2 to "多云", 3 to "阴天",
            45 to "有雾", 48 to "冻雾", 51 to "小毛毛雨", 53 to "毛毛雨", 55 to "大毛毛雨",
            61 to "小雨", 63 to "中雨", 65 to "大雨", 71 to "小雪", 73 to "中雪", 75 to "大雪",
            80 to "阵雨", 81 to "中阵雨", 82 to "大阵雨", 85 to "小阵雪", 86 to "大阵雪",
            95 to "雷暴", 96 to "雷暴伴小冰雹", 99 to "雷暴伴大冰雹"
        )

        fun descriptionFor(code: Int): String = weatherMap[code] ?: "未知"
    }
}

/**
 * Open-Meteo API 响应
 */
data class OpenMeteoResponse(
    val current: CurrentWeather,
    val daily: DailyWeather
) {
    data class CurrentWeather(
        @SerializedName("temperature_2m") val temperature: Double,
        @SerializedName("relative_humidity_2m") val humidity: Int,
        @SerializedName("weather_code") val weatherCode: Int,
        @SerializedName("wind_speed_10m") val windSpeed: Double
    )
    data class DailyWeather(
        @SerializedName("temperature_2m_max") val temperatureMax: List<Double>,
        @SerializedName("temperature_2m_min") val temperatureMin: List<Double>,
        @SerializedName("precipitation_probability_max") val precipitationProb: List<Int>
    )
}

/**
 * 天气服务 - 使用 Open-Meteo 免费API
 */
class WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * 根据经纬度获取实时天气
     */
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&forecast_days=1&timezone=auto"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("天气API错误: ${response.code}"))
            }

            val data = gson.fromJson(body, OpenMeteoResponse::class.java)
            val weather = WeatherData(
                temperature = data.current.temperature,
                temperatureMax = data.daily.temperatureMax.firstOrNull() ?: data.current.temperature,
                temperatureMin = data.daily.temperatureMin.firstOrNull() ?: data.current.temperature,
                humidity = data.current.humidity,
                precipitationProb = data.daily.precipitationProb.firstOrNull() ?: 0,
                weatherCode = data.current.weatherCode,
                windSpeed = data.current.windSpeed,
                description = WeatherData.descriptionFor(data.current.weatherCode)
            )
            Result.success(weather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
