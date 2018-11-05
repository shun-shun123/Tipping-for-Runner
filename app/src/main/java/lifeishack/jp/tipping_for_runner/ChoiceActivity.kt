package lifeishack.jp.tipping_for_runner

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.linecorp.linesdk.LineProfile
import com.linecorp.linesdk.api.LineApiClient
import com.linecorp.linesdk.api.LineApiClientBuilder


//Login後の画面。RunnerとAudienceの選択画面の予定
class ChoiceActivity : AppCompatActivity() {

    var lineApiClient: LineApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice)

        //LineAPIClientの初期化として必要なようです
        val apiClientBuilder = LineApiClientBuilder(applicationContext, "1619051002")
        lineApiClient = apiClientBuilder.build()

        //profile情報をMainActivityより取得
        val profile: LineProfile = intent.getParcelableExtra("line_profile")
        //これで名前とかidとかとれるっぽい
        println("idはこれこれ")
        println(profile.displayName)
        println(profile.userId)

        val toSpectatorButton: Button = findViewById(R.id.audience)
        toSpectatorButton.setOnClickListener {
            val intent: Intent = Intent(this@ChoiceActivity, SpectatorActivity::class.java)
            intent.putExtra("LINE_ID", profile.userId)
            startActivity(intent)
        }

        val toRunnerButton: Button = findViewById(R.id.choice_runner)
        toRunnerButton.setOnClickListener {
            val intent: Intent = Intent(this@ChoiceActivity, RunnerFormActivity::class.java)
            intent.putExtra("LINE_ID", profile.userId)
            intent.putExtra("LINE_NAME", profile.displayName)
            startActivity(intent)
        }
    }
}
