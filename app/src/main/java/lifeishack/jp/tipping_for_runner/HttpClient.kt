package lifeishack.jp.tipping_for_runner

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
        "/marathon".httpGet().response { request, response, result ->
            when (result) {
                is Result.Success -> {
                    responseBody = String(response.data)
                }
                is Result.Failure -> {
                    responseBody = "ERROR"
                }
            }
        }
        return responseBody
    }

    public fun fetchMarathonName() {
        val marathonData: String = downlaodMarathonData()

    }
}