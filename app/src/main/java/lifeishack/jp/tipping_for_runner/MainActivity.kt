package lifeishack.jp.tipping_for_runner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log;
import android.view.View
import android.widget.TextView
import com.linecorp.linesdk.LineApiResponseCode

import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

private val REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loginButton = findViewById<View>(R.id.login_button) as TextView
        loginButton.setOnClickListener { v ->
            try {
                // App-to-app login channelIDはべんちゃのLINE IDで作ったチャンネルのIDです。
                val loginIntent = LineLoginApi.getLoginIntent(v.context, "1619051002");
                startActivityForResult(loginIntent, REQUEST_CODE)

            } catch (e: Exception) {
                Log.e("ERROR", e.toString())
            }
        }
    }
    
    override public fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE) {
            Log.e("ERROR", "Unsupported Request");
            return;
        }

        val result: LineLoginResult = LineLoginApi.getLoginResultFromIntent(data);

        when (result.responseCode) {

            LineApiResponseCode.SUCCESS -> {
                //ChoiceActivityに"line_profile"などの情報を載せつつ遷移
                val transitionIntent = Intent(this, ChoiceActivity::class.java)
                transitionIntent.putExtra("line_profile", result.lineProfile)
                transitionIntent.putExtra("line_credential", result.lineCredential)
                transitionIntent.putExtra("user_id", result.lineProfile?.userId)
                startActivity(transitionIntent)
            }

            LineApiResponseCode.CANCEL -> Log.e("ERROR", "LINE Login Canceled by user!!")

            else -> {
                Log.e("ERROR", "Login FAILED!")
                Log.e("ERROR", result.errorData.toString())
            }
        }

    }


}
