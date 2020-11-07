package com.example.duviservicios

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView

class IntroductoryActivity : AppCompatActivity() {

    var logo: ImageView? = null
    var splashImg: ImageView? = null
    var lottieAnimationView: LottieAnimationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_introductory)

        //logo = findViewById(R.id.logo) as ImageView
        //splashImg = findViewById(R.id.img) as ImageView
        //lottieAnimationView = findViewById(R.id.lottie) as LottieAnimationView

       // splashImg!!.animate().translationY(-1600.toFloat()).setDuration(1000).setStartDelay(4000)
       // logo!!.animate().translationY(1400.toFloat()).setDuration(1000).setStartDelay(4000)
       // lottieAnimationView!!.animate().translationY(1400.toFloat()).setDuration(1000)
         //   .setStartDelay(4000)


        supportActionBar?.hide()

        Handler().postDelayed({
            val intent = Intent(this@IntroductoryActivity,MainActivity::class.java)
            startActivity(intent)
            finish()
        },3000)
    }

}
