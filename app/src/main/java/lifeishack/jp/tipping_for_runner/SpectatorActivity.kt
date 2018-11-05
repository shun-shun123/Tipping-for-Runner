package lifeishack.jp.tipping_for_runner

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.squareup.moshi.Moshi
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.sqrt

class SpectatorActivity : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private val TAG: String = "SpectatorActivity"

    private var lineId: String = ""

    // シェイク検知に必要な定数・変数
    private val SHAKE_TIMEOUT = 500
    private val FORCE_THRESHOLD = 6
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var preAccel: Float = 1.0F

    // <marathonID, marathon Name>
    private var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
    private val baseUrl: String = "https://073af05b.ngrok.io"

    // <marathonID, RunnerName>
    private var allRunnnerData: MutableList<Pair<Int, String>> = mutableListOf()

    internal var mHandler: Handler = Handler()
    internal var mCounter: Int = 0

    private var spinner: Spinner? = null
    private var runnerSpinner: Spinner? = null

    // POSTするのに使うパラメータ
    private var marathonID: Int = 0
    private var runnerId: Int = 0

    private var progressBar: ProgressBar? = null

    init {
        FuelManager.instance.basePath = baseUrl
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spectator)

        progressBar = findViewById(R.id.progressBar)

        lineId = intent.getStringExtra("LINE_ID")
        Log.d("HttpClientTAG", lineId)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        spinner = findViewById(R.id.spinner)
        spinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("SPINNER", "position: ${position}")
                for ((k, v) in allMarathonData) {
                    if (k == position + 1) {
                        marathonID = k
                        downloadRunnerData(marathonID)
                        Log.d("SPINNER", "marathonID: ${marathonID}\nname: ${v}")
                        break
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        runnerSpinner = findViewById(R.id.runnerSpinner)
        runnerSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                for ((k, _) in allRunnnerData) {
                    if (k == position + 1) {
                        runnerId = k
                        break
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        downloadMarathonData()
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
        allMarathonData = mutableListOf()
        allRunnnerData = mutableListOf()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // 取得した値の予期せぬ変更を防ぐために、センサーの値をコピーして以下で利用する
            val values: FloatArray = event.values.clone()

            val accel: Float = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
            val diff: Float = Math.abs(preAccel - accel)
            if (diff > FORCE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if ((now - mLastTime) < SHAKE_TIMEOUT) {
                    // シェイク中
                    mShakeCount++
                } else {
                    AlertDialog.Builder(this)
                            .setTitle("投げ銭しますか？")
                            .setPositiveButton("はい！") {dialog, which ->
                                Log.d(TAG,"投げ銭: ${mShakeCount}")
                                val postContent: PostBody = PostBody(mShakeCount, lineId)
                                val adapter = Moshi.Builder().build().adapter(PostBody::class.java)
                                val jsonObject = adapter.toJson(postContent)
                                "/line/push/${runnerId}".httpPost().jsonBody(jsonObject).response {request, response, result ->
                                    if (result.component2() != null) {
                                        Log.d("HttpClientTAG", "${result.component2()}")
                                    }
                                    when (result) {
                                        is Result.Success -> {
                                            Log.d("HttpClientTAG", "POST Success")
                                        }
                                        is Result.Failure -> {
                                            Log.d("HttpClientTAG", "POST Failed")
                                        }
                                    }
                                }
                            }
                            .setNegativeButton("いいえ") {dialog, which ->

                            }
                            .show()
                    mShakeCount = 0
                }
                mLastTime = now
            }
            preAccel = accel
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d(TAG, "Sensor Accuracy Changed")
    }

    private fun downloadMarathonData() {
        "/marathon".httpGet().responseJson {request, response, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    copyMarathonData(json, "id", "name", allMarathonData)
                    Log.d("HttpClientTAG", "downloadMarathonData is completed")
                    for ((k, _) in allMarathonData) {
                        downloadRunnerData(k)
                        Log.d("HttpClientTAG", "downloading Result: ${allMarathonData}\n${allRunnnerData}")
                    }
//                    downloadRunnerData(marathonID)
//                    Log.d("HttpClientTAG", "downloading Result: ${allMarathonData}\n${allRunnnerData}")
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
                Log.d("HttpClientTAG", "downloadData: ${allMarathonData}\n${allRunnnerData}")

                val adapterContent: MutableList<String> = mutableListOf()
                for ((k, v) in allMarathonData) {
                    adapterContent.add(v)
                }
                val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, adapterContent)

                val runnerContent: MutableList<String> = mutableListOf()
                for ((_, v) in allRunnnerData) {
                    runnerContent.add(v)
                }
                val runnerAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, runnerContent)
                mHandler.post {
                    // この部分はUIスレッドで動作する
                    spinner?.adapter = adapter
                    runnerSpinner?.adapter = runnerAdapter
                    progressBar?.visibility = View.VISIBLE
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        thread.start()
        progressBar?.visibility = View.INVISIBLE

//        doAsync {
//            val (request, response, result) = Fuel.get("/marathon").responseJson()
//            if (result.component2() != null) {
//                Log.d("HttpClientTAG", "ERROR: ${result.component2()}")
//            }
//            when (result) {
//                is Result.Success -> {
//                    val json = result.value.array()
//                    copyMarathonData(json, "id", "name", allMarathonData)
//                    Log.d("HttpClientTAG", "allMarathonDataにコピーしたよ\n${allMarathonData}")
//                }
//                is Result.Failure -> {
//                    Log.d("HttpClientTAG", "ERRORYEHAAAAAAAAAAA")
//                }
//            }
//        }
    }

    private fun downloadRunnerData(marathonID: Int) {
        "/runner/${marathonID}".httpGet().responseJson {request, response, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    Log.d("HttpClientTAG", "runner data(iD: ${marathonID}): ${json}")
                    copyMarathonData(json, "id", "name", allRunnnerData)
                }
                is Result.Failure -> {
                    Log.d("HttpClientTAG", "Faild to downloadRunnerData")
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
                Log.d("HttpClientTAG", "downloadData: ${allMarathonData}\n${allRunnnerData}")

                val runnerContent: MutableList<String> = mutableListOf()
                for ((_, v) in allRunnnerData) {
                    runnerContent.add(v)
                }
                val runnerAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, runnerContent)
                mHandler.post {
                    // この部分はUIスレッドで動作する
                    runnerSpinner?.adapter = runnerAdapter
                    progressBar?.visibility = View.VISIBLE
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        thread.start()
        progressBar?.visibility = View.INVISIBLE
//        doAsync {
//            val (request, response, result) = Fuel.get("/runner/$marathonID").responseJson()
//            if (result.component2() != null) {
//                Log.d("HttpClientTAG", "ERROR(RUNNER): ${result.component2()}")
//            }
//            when (result) {
//                is Result.Success -> {
//                    val json = result.value.array()
//                    copyMarathonData(json, "id", "name", allRunnnerData)
//                    Log.d("HttpClientTAG", "${this@doAsync}: Success")
//                }
//                is Result.Failure -> {
//                    Log.d("HttpClientTAG", "${this@doAsync}: Faild")
//                }
//            }
//        }
    }

    private fun copyMarathonData(jsonArray: JSONArray, component1: String, component2: String, targetList: MutableList<Pair<Int, String>>) {
        // 更新時に前のデータに上書きするため
        targetList.clear()
        for (i in 0..jsonArray.length() - 1) {
            val jsonObj = jsonArray[i] as JSONObject
            targetList.add(Pair(jsonObj.get(component1).toString().toInt(), jsonObj.get(component2).toString()))
        }
    }
}


data class PostBody(val number: Int, val audience_line_id: String)