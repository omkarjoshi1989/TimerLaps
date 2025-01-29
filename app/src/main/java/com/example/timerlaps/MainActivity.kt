package com.example.timerlaps

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var lapButton: Button
    private lateinit var lapListView: ListView

    private var isRunning = false
    private var timeMillis: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    private val lapTimes = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var textToSpeech: TextToSpeech
    private var lapCount = 1

    private val updateTime = object : Runnable {
        override fun run() {
            timeMillis += 10
            updateTimerUI()
            handler.postDelayed(this, 10)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        timerText = findViewById(R.id.timerText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        lapButton = findViewById(R.id.lapButton)
        lapListView = findViewById(R.id.lapListView)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        // Initialize ListView Adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, lapTimes)
        lapListView.adapter = adapter

        startButton.setOnClickListener { startTimer() }
        stopButton.setOnClickListener { stopTimer() }
        resetButton.setOnClickListener { resetTimer() }
        lapButton.setOnClickListener { recordLap() }
    }

    private fun startTimer() {
        if (!isRunning) {
            handler.post(updateTime)
            isRunning = true
            startButton.visibility = Button.GONE
            stopButton.visibility = Button.VISIBLE
            lapButton.visibility = Button.VISIBLE
            resetButton.visibility = Button.GONE
        }
    }

    private fun stopTimer() {
        if (isRunning) {
            handler.removeCallbacks(updateTime)
            isRunning = false
            stopButton.visibility = Button.GONE
            startButton.visibility = Button.VISIBLE
            resetButton.visibility = Button.VISIBLE
        }
    }

    private fun resetTimer() {
        stopTimer()
        timeMillis = 0
        updateTimerUI()
        lapTimes.clear()
        adapter.notifyDataSetChanged()
        lapCount = 1
        lapButton.visibility = Button.GONE
        resetButton.visibility = Button.GONE
    }

    private fun recordLap() {
        val timeFormatted = formatTime(timeMillis)
        lapTimes.add("Lap $lapCount: $timeFormatted")
        adapter.notifyDataSetChanged()
        announceLap(lapCount)
        lapCount++
    }

    private fun announceLap(lap: Int) {
        val message = "Lap $lap"
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun updateTimerUI() {
        timerText.text = formatTime(timeMillis)
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 60000) % 60
        val seconds = (milliseconds / 1000) % 60
        val millis = (milliseconds % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, millis)
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}