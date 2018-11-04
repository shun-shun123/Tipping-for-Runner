package lifeishack.jp.tipping_for_runner

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject

class RunnerFormActivity : AppCompatActivity() {

    var marathonNameText: EditText? = null
    var bibNumberText: EditText? = null
    var submitButton: Button? = null

    private var lineId: String = ""

    private var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
    private val baseUrl: String = "https://d15af500.ngrok.io"

    init {
        FuelManager.instance.basePath = baseUrl
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runner_form)

        marathonNameText = findViewById(R.id.marathonName)
        bibNumberText = findViewById(R.id.bibNumber)
        submitButton = findViewById(R.id.submitButton)

        lineId = intent.getStringExtra("LINE_ID")
        Log.d("HttpClientTAG", lineId)

        submitRunnerInfo()
    }

    fun submitRunnerInfo() {
        //送信ボタンタップ後、APIリクエストをサーバー側に送信？
        submitButton?.setOnClickListener {
            val marathonName = marathonNameText?.text.toString()
            val bibNumber = bibNumberText?.text.toString()
            println(marathonName)
            println(bibNumber)
        }
    }

    override fun onResume() {
        super.onResume()
        allMarathonData = downloadMarathonData()
        Log.d("HttpClientTAG", "onResume: $allMarathonData")
    }

    private fun downloadMarathonData(): MutableList<Pair<Int, String>> {
        doAsync {
            val (request, response, result) = Fuel.get("/marathon").responseJson()
            if (result.component2() != null) {
                Log.d("HttpClientTAG", "ERROR: ${result.component2()}")
            }
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    copyMarathonData(json)
                    Log.d("HttpClientTAG", "allMarathonDataにコピーしたよ\n${allMarathonData}")
                }
                is Result.Failure -> {
                    Log.d("HttpClientTAG", "ERRORYEHAAAAAAAAAAA")
                }
            }
        }
        return allMarathonData
    }

    private fun copyMarathonData(jsonArray: JSONArray) {
        allMarathonData = mutableListOf()
        for (i in 0..jsonArray.length() - 1) {
            val jsonObj = jsonArray[i] as JSONObject
            allMarathonData.add(Pair(jsonObj.get("id").toString().toInt(), jsonObj.get("name").toString()))
        }
    }
}
