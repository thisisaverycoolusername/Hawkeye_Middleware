package com.example.myservice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myservice.ui.theme.MyServiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.your_button_id)
        val buttonGetdata = findViewById<Button>(R.id.getdata)
        buttonGetdata?.setOnClickListener {
            println("get data from pico button clicked!")
            val intent = Intent(this, mainService::class.java)



            startService(intent)
        }

        button?.setOnClickListener {

            println("send data to api button clicked!")
            val intent = Intent(this, mainService::class.java)

            intent.putExtra("data_key", "your_data_here")

            startService(intent)
        } ?: run {

            println("Button not found")
        }
    }
}