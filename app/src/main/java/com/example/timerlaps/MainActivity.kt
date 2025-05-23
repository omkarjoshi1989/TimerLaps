package com.example.timerlaps

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timerlaps.ui.LapsAdapter
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvTotalTime: TextView
    private lateinit var tvCurrentLapTime: TextView
    private lateinit var btnReset: Button
    private lateinit var btnLap: Button
    private lateinit var btnPauseResume: Button
    private lateinit var rvLaps: RecyclerView

    private var tts: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var isRunning = false
    private var startTimeMillis: Long = 0 // When the timer officially started or resumed
    private var timeWhenPausedMillis: Long = 0 // Time accumulated when timer was paused

    private var currentLapStartTimeMillis: Long = 0 // When the current lap started
    private var lapNumberCounter = 0 // Tracks the number of laps recorded

    private val lapList = mutableListOf<Lap>()
    private lateinit var lapsAdapter: LapsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Text-to-Speech
        tts = TextToSpeech(this, this)

        // Initialize UI components
        tvTotalTime = findViewById(R.id.tv_total_time)
        tvCurrentLapTime = findViewById(R.id.tv_current_lap_time)
        btnReset = findViewById(R.id.btn_reset)
        btnLap = findViewById(R.id.btn_lap)
        btnPauseResume = findViewById(R.id.btn_pause_resume)
        rvLaps = findViewById(R.id.rv_laps)

        // Set up RecyclerView
        lapsAdapter = LapsAdapter(lapList)
        rvLaps.layoutManager = LinearLayoutManager(this).apply {
            // Display newest lap at the top
            stackFromEnd = true
            reverseLayout = true
        }
        rvLaps.adapter = lapsAdapter

        // Initialize timer runnable
        timerRunnable = object : Runnable {
            override fun run() {
                val totalElapsedMillis = SystemClock.uptimeMillis() - startTimeMillis + timeWhenPausedMillis
                tvTotalTime.text = formatTime(totalElapsedMillis)

                val currentLapElapsedMillis = SystemClock.uptimeMillis() - currentLapStartTimeMillis
                tvCurrentLapTime.text = "Lap: ${formatTime(currentLapElapsedMillis)}"

                handler.postDelayed(this, 10) // Update every 10 milliseconds
            }
        }

        // Set up button click listeners
        btnLap.setOnClickListener {
            onLapButtonClick()
        }

        btnPauseResume.setOnClickListener {
            onPauseResumeButtonClick()
        }

        btnReset.setOnClickListener {
            onResetButtonClick()
        }

        // Initial state of buttons
        updateButtonStates(false)
    }

    private fun startTimer() {
        if (!isRunning) {
            isRunning = true
            startTimeMillis = SystemClock.uptimeMillis()
            currentLapStartTimeMillis = SystemClock.uptimeMillis() // Start current lap timer as well
            handler.post(timerRunnable)
            updateButtonStates(true)
        }
    }

    private fun pauseTimer() {
        if (isRunning) {
            isRunning = false
            handler.removeCallbacks(timerRunnable)
            timeWhenPausedMillis += (SystemClock.uptimeMillis() - startTimeMillis)
            updateButtonStates(false)
        }
    }

    private fun resetTimer() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)

        startTimeMillis = 0
        timeWhenPausedMillis = 0
        currentLapStartTimeMillis = 0
        lapNumberCounter = 0

        tvTotalTime.text = formatTime(0)
        tvCurrentLapTime.text = "Lap: ${formatTime(0)}"

        lapsAdapter.clearLaps()
        updateButtonStates(false)
    }

    private fun onLapButtonClick() {
        if (!isRunning) {
            // If timer is not running, the first 'Lap' click acts as 'Start'
            startTimer()
            lapNumberCounter = 1 // First actual lap starts, so we name it Lap 1
            speakLapCompletion(lapNumberCounter) // Announce "Lap 1 complete." which means timer started for it.
            // When timer starts, we consider Lap 1 as implicitly started.
            // The *next* click will record Lap 1's duration and start Lap 2.
        } else {
            // Record the current lap
            val currentLapDuration = SystemClock.uptimeMillis() - currentLapStartTimeMillis
            val totalTimeAtThisLap = SystemClock.uptimeMillis() - startTimeMillis + timeWhenPausedMillis

            val newLap = Lap(lapNumberCounter, currentLapDuration, totalTimeAtThisLap)
            lapsAdapter.addLap(newLap)
            rvLaps.scrollToPosition(0) // Scroll to the newest lap

            lapNumberCounter++ // Increment for the next lap
            // Reset current lap timer for the next lap
            currentLapStartTimeMillis = SystemClock.uptimeMillis()

            speakLapCompletion(lapNumberCounter) // Speak the number of the *new* lap starting
        }
    }

    private fun onPauseResumeButtonClick() {
        if (isRunning) {
            pauseTimer()
        } else {
            // If resume after a pause, we need to restart the timer
            // but keep accumulated time
            startTimeMillis = SystemClock.uptimeMillis() // Reset start time for accurate `uptimeMillis()` calculations
            // The `currentLapStartTimeMillis` already correctly holds the starting point for the current lap,
            // so we don't need to adjust it further based on `SystemClock.uptimeMillis()`
            // because `timeWhenPausedMillis` takes care of total elapsed time.
            handler.post(timerRunnable)
            isRunning = true
            updateButtonStates(true)
        }
    }

    private fun onResetButtonClick() {
        resetTimer()
    }

    // Helper function to format milliseconds into HH:MM:SS:ms
    private fun formatTime(millis: Long): String {
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        val hundredths = (millis % 1000) / 10 // Get hundredths of a second

        return String.format("%02d:%02d:%02d", minutes, seconds, hundredths)
    }

    // Text-to-Speech Initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US) // Set your desired language

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                Log.d("TTS", "TTS Initialization successful.")
            }
        } else {
            Log.e("TTS", "TTS Initialization failed!")
        }
    }

    private fun speakLapCompletion(lapNumber: Int) {
        if (tts != null && tts?.isSpeaking == false) {
            val text = "Starting Lap $lapNumber" // Announce the new lap that has just begun
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun updateButtonStates(isTimerRunning: Boolean) {
        if (isTimerRunning) {
            btnLap.text = "Lap"
            btnPauseResume.text = "Pause"
            btnPauseResume.isEnabled = true
            btnReset.isEnabled = true // Can reset when running or paused
        } else {
            // If timer is not running
            if (timeWhenPausedMillis == 0L && lapList.isEmpty()) { // Timer is at reset state
                btnLap.text = "Start" // First lap button click starts
                btnPauseResume.text = "Pause" // Default text
                btnPauseResume.isEnabled = false // Cannot pause if not running
                btnReset.isEnabled = false // Cannot reset if nothing has started
            } else { // Timer is paused
                btnLap.text = "Lap" // Still can record lap on pause, though usually not desired in a stopwatch
                btnPauseResume.text = "Resume"
                btnPauseResume.isEnabled = true
                btnReset.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        // Shut down TTS when activity is destroyed to free up resources
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        // Stop the handler callbacks to prevent memory leaks
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}