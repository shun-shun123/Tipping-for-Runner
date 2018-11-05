package lifeishack.jp.tipping_for_runner

import android.app.Activity
import android.app.ProgressDialog
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import com.squareup.moshi.Json
import kotlinx.android.synthetic.main.activity_runner_form.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.AdapterView.OnItemSelectedListener
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonAdapter
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONArray


class RunnerFormActivity : AppCompatActivity() {

    var marathonSpinner: Spinner? = null
    var bibNumberText: EditText? = null
    var submitButton: Button? = null

    private var lineId: String = ""
    private var lineName: String = ""

    private var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
    private var spinnerItems: ArrayList<String> = ArrayList()
    private val baseUrl: String = "https://tipping-for-runner.herokuapp.com"



    init {
        FuelManager.instance.basePath = baseUrl
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        downloadMarathonData()
        setContentView(R.layout.activity_runner_form)

        var dialog: ProgressDialog

//        dialog.setTitle("読み込み中")
//        dialog.setMessage("読み込み中です。。")
//        dialog.show()

        marathonSpinner = findViewById<Spinner>(R.id.marathonSpinner)
//        spinnerItems = arrayOf("Spinner", "Android", "Apple", "Windows")
        val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        marathonSpinner!!.adapter = adapter


        bibNumberText = findViewById(R.id.bibNumber)
        submitButton = findViewById(R.id.submitButton)
        lineId = intent.getStringExtra("LINE_ID")
        lineName = intent.getStringExtra("LINE_NAME")
        println("これがRunnerFormで確認できたid")
        println(lineId)
        println(lineName)
        println("1::" + allMarathonData)

        submitRunnerInfo()
    }



    fun submitRunnerInfo() {
        //送信ボタンタップ後、APIリクエストをサーバー側に送信？
        submitButton?.setOnClickListener {
            val marathonName = marathonSpinner!!.selectedItem.toString()
            val bibNumber = bibNumberText?.text.toString()
            println(marathonName)
            println(bibNumber)
            println(lineId)
            println(lineName)
//            postRunnerData(marathonName, bibNumber, lineId)
        }
    }



    private fun downloadMarathonData(): MutableList<Pair<Int, String>> {
        doAsync {
            var json :JSONArray
            val (request, response, result) = Fuel.get("/marathon").responseJson()
            if (result.component2() != null) {
                Log.d("HttpClientTAG", "ERROR: ${result.component2()}")
            }
            when (result) {
                is Result.Success -> {
                    json = result.value.array()
                    copyMarathonData(json)
                }
                is Result.Failure -> {
                }
            }
        }
        return allMarathonData
    }

    private fun copyMarathonData(jsonArray: JSONArray) {
        allMarathonData = mutableListOf()
        for (i in 0..jsonArray.length() - 1) {
            val jsonObj = jsonArray[i] as JSONObject
            spinnerItems.add(jsonObj.get("name").toString())
            allMarathonData.add(Pair(jsonObj.get("id").toString().toInt(), jsonObj.get("name").toString()))
        }
        println("copyMarathonData")
        println(spinnerItems)
        println(allMarathonData)
    }


//    private fun downloadMarathonData() {
//        val builder = Uri.Builder()
//        val latch = CountDownLatch(1)
//        val task = getMarathonRequest()
//        task.execute();
//
//    }

//    inner class getMarathonRequest(): AsyncTask<Uri.Builder, Void, Void>() {
//        override fun doInBackground(vararg builder: Uri.Builder): Void? {
//
////            val url = URL("https://073af05b.ngrok.io/marathon")
//            val url = URL("https://tipping-for-runner.herokuapp.com/marathon")
//            val http = url.openConnection() as HttpURLConnection
//
//
//            try {
//                println("start")
//                http.addRequestProperty("Content-Type", "application/json; charset=UTF-8")
//                http.setRequestMethod("GET")
//                http.doInput = true
//                http.connect()
//
//                println("response,reader,resultです")
//                val responseCode = http.responseCode
//                println(responseCode)
//                val ret = http.inputStream
//                val reader: BufferedReader = BufferedReader(InputStreamReader(ret))
//                val result = reader.readLine()
//                println("result")
//                val splitFirst = result.split("[")
//                val splitSecond = splitFirst[1].split("]")
////                println(splitSecond)
////                println(splitSecond[0])
//
//
//                val runnerList = splitSecond[0].split(",")
//                println("runnerList")
//                println(runnerList)
//                println("size")
//                println(runnerList[0])
//                println(runnerList.size)
//                println(runnerList[1])
////                for(list in runnerList) {
////                    val JsonObj = JSONObject(list)
////                    println(JsonObj)
////                    println(JsonObj["id"])
////                }
//
//                //ここからテスト
////                val parentJsonObj = JSONObject(splitSecond[0])
////                println("parentJsonObj")
////                println(parentJsonObj)
//                return null
//            } catch (e: InterruptedException) {
//                println("Interruptです")
//                println(e)
//                return null
//            } finally {
//                http.disconnect()
//                println("finallyです")
//                return null
//            }
//            return null
//        }
//
//        override fun onPostExecute(result: Void?) {
//            super.onPostExecute(result)
//            println("onPostExecute")
//        }
//    }



    private fun postRunnerData(marasonName: String, bibNumber: String, lineId: String) {
        val builder = Uri.Builder()
        val task = runnerFormRequest(this, marasonName, bibNumber, lineId)
        task.execute(builder);
    }

    inner class runnerFormRequest(activity: Activity, marasonName: String, bibNumber: String, lineId: String): AsyncTask<Uri.Builder, Void, Void>() {

        private var mainActivity: Activity? = activity
        private var name: String = ""
        private var number: String = bibNumber
        private var marathon_id: Int = 111
        private var line_user_id: String = lineId

        private var numberKari: Int = 123456
        override fun doInBackground(vararg builder: Uri.Builder): Void? {
            // httpリクエスト投げる処理を書く。

            val url = URL("https://073af05b.ngrok.io/runner")
            val http = url.openConnection() as HttpURLConnection
            var result: String = ""

            val json = """
            {
                "name": "${name}",
                "number": ${numberKari},
                "marathon_id": "${marathon_id}",
                "line_user_id": "${line_user_id}"
            }
            """

            try {
                http.addRequestProperty("Content-Type", "application/json; charset=UTF-8")
                http.setRequestMethod("POST");
                http.doOutput = true
                http.connect()

                val os: OutputStream = http.outputStream
                val ps: PrintStream = PrintStream(os)
                ps.print(json)

                println("response,reader,resultです")
                val responseCode = http.responseCode
                println(responseCode)
                val ret = http.inputStream
                val reader: BufferedReader = BufferedReader(InputStreamReader(ret))
                println(reader)
                result = reader.readLine()
                println(result)

                return null
            } catch (e: InterruptedException) {
                println("Interruptです")
                println(e)
                return null
            } finally {
                http.disconnect()
                println("finallyです")
                return null
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            println("onPostExecute")
        }


    }

}
