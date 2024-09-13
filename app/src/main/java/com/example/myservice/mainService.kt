package com.example.myservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class mainService : Service() {


    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            fetchDataFromPicoAsync()
            handler.postDelayed(this, 30000)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onstartcommand")
        val dataFromPico = intent?.getStringExtra("data_key") ?: return START_NOT_STICKY
        sendDataOverMobileData(this, dataFromPico)
        println("about to run getDataFromRpiPico")
        fetchDataFromPicoAsync()
        handler.post(runnable)
        //getDataFromRpiPico()
        return START_STICKY
    }

    //async
    fun fetchDataFromPicoAsync(): String? {
        var result: String? = null
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    println("inside the function getDataFromRpiPico")

                    val url = URL("http://192.168.4.1/temp") // Pico's API endpoint
                    val urlConnection = url.openConnection() as HttpURLConnection
                    urlConnection.connectTimeout = 5000
                    urlConnection.readTimeout = 5000
                    urlConnection.requestMethod = "GET"

                    val responseCode = urlConnection.responseCode
                    println("Response Code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = BufferedReader(InputStreamReader(urlConnection.inputStream))
                        val response = StringBuilder()
                        var line: String?

                        while (inputStream.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        inputStream.close()
                        sendDataOverMobileData(this@mainService, response.toString() )
                        println("Data from Pico: $response")
                        result = response.toString()
                    } else {
                        println("Failed to get data: $responseCode")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("Error fetching data from Pico: ${e.message}")
                }
            }
        }
        return result
    }


    fun sendDataOverMobileData(context: Context, dataFromPico: String) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        println("sendDataOverMobileData")

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()


        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onAvailable(network: Network) {
                super.onAvailable(network)


                try {
                    val url = URL("https://webhook.site/0bf9cc56-4dc7-43a8-9905-e3a31913b71d") // Replace with your server URL
                    val urlConnection = network.openConnection(url) as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.doOutput = true


                    val outputStream = OutputStreamWriter(urlConnection.outputStream)
                    outputStream.write("data=$dataFromPico")
                    outputStream.flush()
                    outputStream.close()
                    


                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        println("Data sent successfully")
                    } else {
                        println("Failed to send data: $responseCode")
                    }

                }

                catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onUnavailable() {
                // Handle case where mobile data is unavailable
                println("Mobile data network is unavailable")
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop the periodic execution
        handler.removeCallbacks(runnable)
    }
}