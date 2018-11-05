package lifeishack.jp.tipping_for_runner

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import org.jetbrains.anko.doAsync
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
    private val baseUrl: String = "https://d15af500.ngrok.io"

    // <marathonID, RunnerName>
    private var allRunnnerData: MutableList<Pair<Int, String>> = mutableListOf()

    init {
        FuelManager.instance.basePath = baseUrl
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spectator)

        lineId = intent.getStringExtra("LINE_ID")
        Log.d("HttpClientTAG", lineId)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        downloadMarathonData()
        for ((k, v) in allMarathonData) {
            downloadRunnerData(k)
        }
        Log.d("HttpClientTAG", "onResume: $allMarathonData")
        Log.d("HttpClientTAG", "onResume: $allRunnnerData")
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
        doAsync {
            val (request, response, result) = Fuel.get("/marathon").responseJson()
            if (result.component2() != null) {
                Log.d("HttpClientTAG", "ERROR: ${result.component2()}")
            }
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    copyMarathonData(json, "id", "name", allMarathonData)
                    Log.d("HttpClientTAG", "allMarathonDataにコピーしたよ\n${allMarathonData}")
                }
                is Result.Failure -> {
                    Log.d("HttpClientTAG", "ERRORYEHAAAAAAAAAAA")
                }
            }
        }
    }

    private fun downloadRunnerData(marathonID: Int) {
        doAsync {
            val (request, response, result) = Fuel.get("/runner/$marathonID").responseJson()
            if (result.component2() != null) {
                Log.d("HttpClientGET", "ERROR(RUNNER): ${result.component2()}")
            }
            when (result) {
                is Result.Success -> {
                    val json = result.value.array()
                    copyMarathonData(json, "id", "name", allRunnnerData)
                    Log.d("HttpClientTAG", "${this@doAsync}: Success")
                }
                is Result.Failure -> {
                    Log.d("HttpClientTAG", "${this@doAsync}: Faild")
                }
            }
        }
    }

    private fun copyMarathonData(jsonArray: JSONArray, component1: String, component2: String, targetList: MutableList<Pair<Int, String>>) {
        for (i in 0..jsonArray.length() - 1) {
            val jsonObj = jsonArray[i] as JSONObject
            targetList.add(Pair(jsonObj.get(component1).toString().toInt(), jsonObj.get(component2).toString()))
        }
    }
}
