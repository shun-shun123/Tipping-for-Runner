package lifeishack.jp.tipping_for_runner

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
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

    private var allMarathonData: MutableList<Pair<Int, String>> = mutableListOf()
    private val baseUrl: String = "https://d15af500.ngrok.io"

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
        allMarathonData = downloadMarathonData()
        Log.d("HttpClientTAG", "onResume: $allMarathonData")
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
                    Log.d(TAG, "${mShakeCount} times shaken")
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
