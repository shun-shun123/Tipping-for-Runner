package lifeishack.jp.tipping_for_runner

import android.app.Activity
import android.app.ProgressDialog
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URL


class RunnerFormActivity : AppCompatActivity() {

    var marathonSpinner: Spinner? = null
    var bibNumberText: EditText? = null
    var submitButton: Button? = null


    private var lineId: String = ""
    private var lineName: String = ""
    private var marathonID: Int = 0

    internal var mHandler: Handler = Handler()
    internal var mCounter: Int = 0

    private var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
    private var spinnerItems: ArrayList<String> = ArrayList()
    private val baseUrl: String = "https://073af05b.ngrok.io"



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

        marathonSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                for ((k, v) in allMarathonData) {
                    if (k == position + 1) {
                        marathonID = k
                        break
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }




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
            println(bibNumber)
            println(lineId)
            println(lineName)
            println(marathonID)
            println("postRunnerData")
            postRunnerData(bibNumber)
        }
    }



    private fun downloadMarathonData() {
        "/marathon".httpGet().responseJson {request, response, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    copyMarathonData(json, "id", "name", allMarathonData)
                    Log.d("HttpClientTAG", "downloadMarathonData is completed: ${allMarathonData}")
                }
                is Result.Failure -> {
                    Log.d("HttpClientTAG", "downloadMarathonData is Failed")
                }
            }
        }
        val thread = Thread(Runnable {
            try {
                mCounter = 0
                while (mCounter < 5) {
                    // ここで時間稼ぎ
                    Thread.sleep(1000)
                    Log.d("HttpClientTAG", "Thread Waiting: ${mCounter}")
                    mCounter++
                }
                // 繰り返しが終わったところで次のActivityに遷移する
                mHandler.post {
                    // この部分はUIスレッドで動作する
                    val contentList: MutableList<String> = mutableListOf()
                    for ((_, v) in allMarathonData) {
                        contentList.add(v)
                    }
                    val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, contentList)
                    marathonSpinner?.adapter = adapter
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        thread.start()
    }

    private fun copyMarathonData(jsonArray: JSONArray, component1: String, component2: String, targetList: MutableList<Pair<Int, String>>) {
        // 更新時に前のデータに上書きするため
        targetList.clear()
        for (i in 0..jsonArray.length() - 1) {
            val jsonObj = jsonArray[i] as JSONObject
            targetList.add(Pair(jsonObj.get(component1).toString().toInt(), jsonObj.get(component2).toString()))
        }
    }

    private fun postRunnerData(bibNumber: String) {
        val builder = Uri.Builder()
        val task = runnerFormRequest(bibNumber)
        task.execute(builder);
    }

    inner class runnerFormRequest(bibNumber: String): AsyncTask<Uri.Builder, Void, Void>() {

        private var line_name: String = lineName
        private var bibInt: Int = Integer.parseInt(bibNumber)
        private var marathon_id: Int = marathonID
        private var line_user_id: String = lineId

        override fun doInBackground(vararg builder: Uri.Builder): Void? {
            // httpリクエスト投げる処理を書く。

            val url = URL("https://073af05b.ngrok.io/runner")
            val http = url.openConnection() as HttpURLConnection
            var result: String = ""


            val json = """
            {
                "name": "${line_name}",
                "number": ${bibInt},
                "marathon_id": "${marathon_id}",
                "runner_line_id": "${line_user_id}"
            }
            """
            println("json")
            println(json)
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
