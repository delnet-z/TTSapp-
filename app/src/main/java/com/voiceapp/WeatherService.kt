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

        // wttr.in WWO weather codes -> 中文
        private val wwoWeatherMap = mapOf(
            "113" to "晴天", "116" to "多云", "119" to "阴天", "122" to "阴天",
            "143" to "有雾", "149" to "霾", "176" to "阵雨", "179" to "阵雪", "182" to "雨夹雪",
            "185" to "冻雨", "200" to "雷暴", "227" to "小雪", "230" to "大雪",
            "248" to "有雾", "260" to "冻雾", "263" to "小雨", "266" to "小雨",
            "281" to "冻雨", "284" to "冻雨", "293" to "小雨", "296" to "小雨",
            "299" to "大雨", "302" to "中雨", "305" to "大雨", "308" to "大雨",
            "311" to "冻雨", "314" to "冻雨", "317" to "雨夹雪", "320" to "雨夹雪",
            "323" to "小雪", "326" to "小雪", "329" to "中雪", "332" to "中雪",
            "335" to "大雪", "338" to "大雪", "350" to "冰雹", "353" to "阵雨",
            "356" to "大雨", "359" to "大雨", "362" to "雨夹雪", "365" to "雨夹雪",
            "368" to "小雪", "371" to "大雪", "374" to "冰雹", "377" to "冰雹",
            "386" to "雷暴", "389" to "雷暴", "392" to "雷暴伴冰雹", "395" to "大雪"
        )

        fun descriptionFor(code: Int): String = weatherMap[code] ?: "未知"

        fun descriptionForWwo(code: String): String = wwoWeatherMap[code] ?: "未知"
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
 * wttr.in API 响应模型
 */
data class WttrResponse(
    @SerializedName("current_condition") val currentCondition: List<WttrCurrent>,
    val weather: List<WttrWeather>
) {
    data class WttrCurrent(
        @SerializedName("temp_C") val tempC: String,
        @SerializedName("weatherCode") val weatherCode: String,
        val humidity: String,
        @SerializedName("windspeedKmph") val windSpeedKmph: String
    )
    data class WttrWeather(
        @SerializedName("maxtempC") val maxTempC: String,
        @SerializedName("mintempC") val minTempC: String,
        val hourly: List<WttrHourly>
    )
    data class WttrHourly(
        @SerializedName("chanceofrain") val chanceOfRain: String?
    )
}

/**
 * 天气服务 - Open-Meteo 主源 + wttr.in 备用
 */
class WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * 获取天气 - 先 Open-Meteo，失败则 fallback wttr.in
     */
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        // 主源：Open-Meteo
        val openMeteo = tryGetOpenMeteo(lat, lon)
        if (openMeteo != null) return@withContext Result.success(openMeteo)

        // 备用：wttr.in
        val wttr = tryGetWttr(lat, lon)
        if (wttr != null) return@withContext Result.success(wttr)

        Result.failure(Exception("所有天气源均不可用"))
    }

    private suspend fun tryGetOpenMeteo(lat: Double, lon: Double): WeatherData? {
        return try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&forecast_days=1&timezone=auto"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null

            val data = gson.fromJson(body, OpenMeteoResponse::class.java)
            WeatherData(
                temperature = data.current.temperature,
                temperatureMax = data.daily.temperatureMax.firstOrNull() ?: data.current.temperature,
                temperatureMin = data.daily.temperatureMin.firstOrNull() ?: data.current.temperature,
                humidity = data.current.humidity,
                precipitationProb = data.daily.precipitationProb.firstOrNull() ?: 0,
                weatherCode = data.current.weatherCode,
                windSpeed = data.current.windSpeed,
                description = WeatherData.descriptionFor(data.current.weatherCode)
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryGetWttr(lat: Double, lon: Double): WeatherData? {
        return try {
            val url = "https://wttr.in/${lat},${lon}?format=j1"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null

            val data = gson.fromJson(body, WttrResponse::class.java)
            val cur = data.currentCondition.firstOrNull() ?: return null
            val today = data.weather.firstOrNull() ?: return null

            // 取逐小时降水概率最大值
            val maxRain = today.hourly
                .mapNotNull { it.chanceOfRain?.toIntOrNull() }
                .maxOrNull() ?: 0

            WeatherData(
                temperature = cur.tempC.toDoubleOrNull() ?: return null,
                temperatureMax = today.maxTempC.toDoubleOrNull() ?: return null,
                temperatureMin = today.minTempC.toDoubleOrNull() ?: return null,
                humidity = cur.humidity.toIntOrNull() ?: 0,
                precipitationProb = maxRain,
                weatherCode = 0,  // wttr.in 用不同的编码体系
                windSpeed = cur.windSpeedKmph.toDoubleOrNull() ?: 0.0,
                description = WeatherData.descriptionForWwo(cur.weatherCode)
            )
        } catch (e: Exception) {
            null
        }
    }
}
