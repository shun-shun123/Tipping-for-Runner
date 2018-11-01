package lifeishack.jp.tipping_for_runner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.linecorp.linesdk.LineApiResponse
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

        val intent = this.intent
        //profile情報をMainActivityより取得
        val profile: LineProfile = intent.getParcelableExtra("line_profile")
        //これで名前とかidとかとれるっぽい
        println(profile.displayName)
        println(profile.userId)



    }
}
