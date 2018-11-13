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
import android.view.WindowManager
import android.widget.*
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

    val serverVariables = ServerVariables()

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private val TAG: String = "HttpClientTAG"
    private var progbarBackground: ImageView? = null

    private var lineId: String = ""

    // シェイク検知に必要な定数・変数
    private val SHAKE_TIMEOUT = 600
    private val FORCE_THRESHOLD = 4
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var preAccel: Float = 1.0F
    private var isLocked: Boolean = true;

    // <marathonID, marathon Name>
    private var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
    // <marathonID, RunnerName>
    private var allRunnerData: MutableList<Pair<Int, String>> = mutableListOf()

    internal var mHandler: Handler = Handler()
    internal var mCounter: Int = 0

    private var marathonDataSpinner: Spinner? = null
    private var runnerListView: ListView? = null
    private var runnerListViewAdapter: RunnerListCustomAdapter? = null
    private var selectedView: View? = null

    // POSTするのに使うパラメータ
    private var marathonID: Int = 0
    private var runnerId: Int = 0

    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spectator)

        progbarBackground = findViewById(R.id.progressbar_background)
        progressBar = findViewById(R.id.progressBar)
        marathonDataSpinner = findViewById(R.id.spinner)
        runnerListView = findViewById(R.id.runners)
        lineId = intent.getStringExtra("LINE_ID")
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        FuelManager.instance.basePath = serverVariables.url

        Toast.makeText(this@SpectatorActivity, "端末を振ってランナーを応援！", Toast.LENGTH_LONG).show()

        marathonDataSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                marathonID = position + 1
                downloadRunnerData(marathonID)
            }
        }

        runnerListView?.setOnItemClickListener { adapterView, view, position, id ->
            selectedView?.setBackgroundResource(R.color.white)
            selectedView = view
            selectedView?.setBackgroundResource(R.color.colorPrimary)
            var index = 0
            for ((k, v) in allRunnerData) {
                if (index == position) {
                    runnerId = k
                    Log.d(TAG, "${v} is selected")
                }
                index++
            }
            isLocked = false
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
                    if (!isLocked) {
                                isLocked = true
                                confirmSending().show()
                    }
                    mShakeCount = 0
                }
                mLastTime = now
            }
            preAccel = accel
        }
    }

    private fun confirmSending(): AlertDialog.Builder =
            AlertDialog.Builder(this)
                .setTitle("投げ銭しますか？")
                .setPositiveButton("はい！") { _, _ ->
                    Log.d(TAG, "投げ銭Count: ${mShakeCount}\nRunnerID: $runnerId")
                    val postContent = PostBody(mShakeCount, lineId)
                    val adapter = Moshi.Builder().build().adapter(PostBody::class.java)
                    val jsonObject = adapter.toJson(postContent)
                    "/line/push/${runnerId}".httpPost().jsonBody(jsonObject).response { _, _, result ->
                        if (result.component2() != null) {
                            Log.d(TAG, "POST Error: ${result.component2()}")
                        }
                        when (result) {
                            is Result.Success -> {
                                Log.d(TAG, "POST Success")
                                Toast.makeText(this@SpectatorActivity, "投げ銭完了！", Toast.LENGTH_LONG).show()
                            }
                            is Result.Failure -> Log.d(TAG, "POST Failed")
                        }
                        isLocked = false
                    }
                }
                .setNegativeButton("いいえ") { _, _ ->
                    isLocked = false
                }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor Accuracy Changed")
    }

    private fun downloadMarathonData() {
        "/marathon".httpGet().responseJson {_, _, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    copyMarathonData(json, "id", "name", allMarathonData)
                    Log.d(TAG, "downloadMarathonData is completed")
                }
                is Result.Failure -> Log.d("HttpClientTAG", "downloadMarathonData is Failed")
            }
        }

        val thread = Thread(Runnable {
            try {
                mCounter = 0
                mHandler.post {
                    // marathonDataを更新すると、spinnerの情報も更新され、結果的に新しくマラソンを選択することになる。
                    // spinnerで選択すると、downloadRunnerDataが走るようになっているため、ここのスレッドではprogressbarもprogbarBackgroundもVISIBLEのままでOK
                    progressBar?.visibility = View.VISIBLE
                    progbarBackground?.visibility = View.VISIBLE
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
                while (mCounter < 2) {
                    // ここで時間稼ぎ
                    Thread.sleep(1000)
                    Log.d(TAG, "Thread Waiting: ${mCounter}")
                    mCounter++
                }
                val marathonDataList: MutableList<String> = mutableListOf()
                for ((_, v) in allMarathonData) {
                    marathonDataList.add(v)
                }
                val marathonSpinnerAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, marathonDataList)
                marathonSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                mHandler.post {
                    // この部分はUIスレッドで動作する
                    marathonDataSpinner?.adapter = marathonSpinnerAdapter
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        thread.start()
    }

    private fun downloadRunnerData(marathonID: Int) {
        "/runner/$marathonID".httpGet().responseJson {_, _, result ->
            when (result) {
                is Result.Success -> copyMarathonData(result.value.array(), "id", "name", allRunnerData)
                is Result.Failure -> Log.d(TAG, "Faild to downloadRunnerData")
            }
        }

        val thread = Thread(Runnable {
            try {
                mCounter = 0
                mHandler.post {
                    progressBar?.visibility = View.VISIBLE
                    progbarBackground?.visibility = View.VISIBLE
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
                while (mCounter < 2) {
                    // ここで時間稼ぎ
                    Thread.sleep(1000)
                    Log.d(TAG, "Thread Waiting: ${mCounter}")
                    mCounter++
                }
                Log.d(TAG, "downloadData: ${allMarathonData}\n${allRunnerData}")
                val runnerDataList: MutableList<CustomListData> = mutableListOf()
                for ((k, v) in allRunnerData) {
                    runnerDataList.add(CustomListData(k.toString(), allMarathonData[marathonDataSpinner?.selectedItemPosition!!].component2(), v))
                }
                runnerListViewAdapter = RunnerListCustomAdapter(this, runnerDataList)
                mHandler.post {
                    // この部分はUIスレッドで動作する
                    runnerListView?.adapter = runnerListViewAdapter
                    progressBar?.visibility = View.INVISIBLE
                    progbarBackground?.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
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
}


data class PostBody(val number: Int, val audience_line_id: String)