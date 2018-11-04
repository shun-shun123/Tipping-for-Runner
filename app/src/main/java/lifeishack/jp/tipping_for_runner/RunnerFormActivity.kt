package lifeishack.jp.tipping_for_runner

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText

class RunnerFormActivity : AppCompatActivity() {

    var marathonNameText: EditText? = null
    var bibNumberText: EditText? = null
    var submitButton: Button? = null
    private val httpClient = HttpClient()
    private val allMarathonData: MutableList<Pair<Int, String>> = httpClient.downlaodMarathonData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runner_form)
        Log.d("HttpClientTAG", "$allMarathonData")

        marathonNameText = findViewById(R.id.marathonName)
        bibNumberText = findViewById(R.id.bibNumber)
        submitButton = findViewById(R.id.submitButton)

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
}
