package lifeishack.jp.tipping_for_runner

import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result

class HttpClient {
    private val TAG: String = "HttpClientTAG"
    private val baseUrl: String = "https://0cb51a24.ngrok.io"

    init {
        FuelManager.instance.basePath = baseUrl
    }

    private fun downlaodMarathonData(): String {
        var responseBody: String = ""
        "/marathon".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.value.obj()
                    Log.d(TAG, "$result")
                    Log.d(TAG, "${json["id"]}")
//                    val results = json.get("body") as JSONArray
//                    val data1 = results[0] as JSONObject
                    Log.d(TAG, "response: ${response}")
                }
                is Result.Failure -> {
                    responseBody = "ERROR"
                }
            }
        }
        Log.d(TAG, responseBody)
        return responseBody
    }

    public fun fetchMarathonName() {
        val marathonData: String = downlaodMarathonData()
    }
}


data class MarathonID(var id: Long, var name: String, var created_at: String, var updated_at: String)