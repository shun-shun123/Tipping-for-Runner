package lifeishack.jp.tipping_for_runner

import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONObject

class HttpClient {
    private val TAG: String = "HttpClientTAG"
    private val baseUrl: String = "https://0cb51a24.ngrok.io"

    init {
        FuelManager.instance.basePath = baseUrl
    }

    public fun downlaodMarathonData(): MutableList<Pair<Int, String>> {
        var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
        "/marathon".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    allMarathonData = copyMarathonData(json)
                }
                is Result.Failure -> {
                }
            }
        }
        Log.d(TAG, "$allMarathonData")
        return allMarathonData
    }

    private fun copyMarathonData(jsonArray: JSONArray): MutableList<Pair<Int, String>> {
        val allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
        for (i in 0..jsonArray.length() - 1) {
            val jsonObj = jsonArray[i] as JSONObject
            allMarathonData.add(Pair(jsonObj.get("id").toString().toInt(), jsonObj.get("name").toString()))
        }
        return allMarathonData
    }
}


data class MarathonID(var id: Long, var name: String)