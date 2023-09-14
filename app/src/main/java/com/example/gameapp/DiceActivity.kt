package com.example.gameapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gameapp.model.Roll
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class DiceActivity : AppCompatActivity(), SensorEventListener {
    // Sensor variables
    private lateinit var sensorManager : SensorManager
    private lateinit var accelerometerSensor : Sensor

    // Values for checking motion
    private var ac : Float = 10F
    private var acCurrent : Float = 0F
    private var acLast : Float = 0F
    private var motionActive : Boolean = false

    // Button variables
    private lateinit var clickButton : Button
    private lateinit var speechButton : Button
    private lateinit var motionButton : Button
    private lateinit var reduceButton : Button
    private lateinit var addButton : Button
    private lateinit var statisticsButton : Button
    private lateinit var throwButton : Button
    private lateinit var logoutButton : Button

    // ImageView variables + linearlayout where some of the images are shown
    private lateinit var diceImage1 : ImageView
    private lateinit var diceImage2 : ImageView
    private lateinit var diceImage3 : ImageView
    private lateinit var diceImage4 : ImageView
    private lateinit var diceImage5 : ImageView
    private lateinit var diceImage6 : ImageView
    private lateinit var row : LinearLayout
    private lateinit var hint : TextView

    // Initialized variables
    private var chosenDice : ArrayList<ImageView> = ArrayList()
    private var activeButton : Button? = null
    private var micPermission : Boolean = true
    private var dices : Int = 1

    // Speech recognition variables
    private lateinit var speechRecognizer : SpeechRecognizer
    private lateinit var speechRecognizerIntent : Intent

    // Firebase variables
    private lateinit var db : FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var id: String
    private var query: Query? = null
    private var adapter: RollAdapter? = null
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)


        // Check permission to use audio
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            micPermission = false
            checkPermission()
        }

        // Initialize components
        clickButton = findViewById(R.id.clickButton)
        speechButton = findViewById(R.id.speechButton)
        motionButton = findViewById(R.id.motionButton)
        reduceButton = findViewById(R.id.reduceButton)
        addButton = findViewById(R.id.addButton)
        logoutButton = findViewById(R.id.logoutButton)
        statisticsButton = findViewById(R.id.statisticsButton)
        throwButton = findViewById(R.id.throwButton)

        diceImage1 = findViewById(R.id.diceImage1)
        diceImage1.setImageResource(R.drawable.one)
        diceImage1.setOnClickListener { addDice(diceImage1) }

        diceImage2 = findViewById(R.id.diceImage2)
        diceImage2.setImageResource(R.drawable.one)
        diceImage2.setOnClickListener { addDice(diceImage2) }

        diceImage3 = findViewById(R.id.diceImage3)
        diceImage3.setImageResource(R.drawable.one)
        diceImage3.setOnClickListener { addDice(diceImage3) }

        diceImage4 = findViewById(R.id.diceImage4)
        diceImage4.setImageResource(R.drawable.one)
        diceImage4.setOnClickListener { addDice(diceImage4) }

        diceImage5 = findViewById(R.id.diceImage5)
        diceImage5.setImageResource(R.drawable.one)
        diceImage5.setOnClickListener { addDice(diceImage5) }

        diceImage6 = findViewById(R.id.diceImage6)
        diceImage6.setImageResource(R.drawable.one)
        diceImage6.setOnClickListener { addDice(diceImage6) }

        clickButton.setBackgroundColor(Color.GRAY)
        speechButton.setBackgroundColor(Color.GRAY)
        motionButton.setBackgroundColor(Color.GRAY)

        logoutButton.setBackgroundColor(Color.rgb(222, 147, 159))

        statisticsButton.setBackgroundColor(Color.rgb(222, 147, 159))
        statisticsButton.setOnClickListener { showStatistics() }

        throwButton.setBackgroundColor(Color.rgb(222, 147, 159))
        throwButton.visibility = View.GONE
        throwButton.setOnClickListener { throwDice() }

        reduceButton.setBackgroundColor(Color.GRAY)
        reduceButton.setOnClickListener { handleDices(-1) }

        addButton.setBackgroundColor(Color.rgb(222, 147, 159))
        addButton.setOnClickListener { handleDices(1) }

        hint = findViewById(R.id.textCommand)
        hint.visibility = View.GONE

        // Make the linearlayout correct size
        row = findViewById(R.id.rowOne)
        val rowNewHeight = 700
        val rowParams = row.layoutParams
        rowParams.height = rowNewHeight
        row.layoutParams = rowParams

        // Initialize the current active dices
        chosenDice.add(diceImage1)

        // Initialize firebase
        db = Firebase.firestore
        auth = Firebase.auth
        id = intent.getStringExtra("userId").toString()

        query = db.collection(id)
            .orderBy("time", Query.Direction.DESCENDING)

        // Activate motion sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, 5000000)
        }

        // Get initial speed values for checking if user is shaking their phone
        acCurrent = SensorManager.GRAVITY_EARTH
        acLast = SensorManager.GRAVITY_EARTH

        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10000)

        // Setup speech recognizer
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                Log.d("Speech", "Ready for speech")
            }

            /**
             * User starts speaking
             */
            override fun onBeginningOfSpeech() {
                Log.d("Speech", "hearing speech")
            }

            override fun onRmsChanged(p0: Float) {
                //
            }

            override fun onBufferReceived(p0: ByteArray?) {
                //
            }

            /**
             * User stops speaking
             */
            override fun onEndOfSpeech() {
                Log.d("Speech", "not hearing speech")
            }

            override fun onError(p0: Int) {
                //
            }

            /**
             * Check if the user has said the correct command to throw dice
             */
            override fun onResults(p0: Bundle) {
                val valid = "throw"
                val result = p0.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (result != null) {
                    for (res : String in result) {
                        if (res == valid) {
                            throwDice()
                            break
                        }
                        if (res.length < 8 && (res.contains("row") || res.contains("rew") || res.contains("rough"))) {
                            throwDice()
                            break
                        }
                    }
                }

                activeButton?.setBackgroundColor(Color.GRAY)
                activeButton = null
                hint.visibility = View.GONE
                hint.text = ""
                speechRecognizer.stopListening()

                Toast.makeText(
                    baseContext,
                    "Voice recording deactivated",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onPartialResults(p0: Bundle?) {
                //
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                //
            }
        })

        // Set onClick action for activating the clicking recognition
        clickButton.setOnClickListener {
            if (activeButton != clickButton || activeButton == null) {
                activeButton?.setBackgroundColor(Color.GRAY)
                clickButton.setBackgroundColor(Color.rgb(222, 147, 159))
                activeButton = clickButton
                sensorManager.unregisterListener(this)
                speechRecognizer.stopListening()
                throwButton.visibility = View.VISIBLE
                hint.visibility = View.GONE
                motionActive = false
                throwButton.setOnClickListener{
                    throwDice()
                }
            }
        }

        // Set onClick action for activating the speech recognition
        speechButton.setOnClickListener {
            if ((activeButton != speechButton || activeButton == null) && micPermission) {
                activeButton?.setBackgroundColor(Color.GRAY)
                speechButton.setBackgroundColor(Color.rgb(222, 147, 159))
                activeButton = speechButton
                sensorManager.unregisterListener(this)
                throwButton.visibility = View.GONE
                throwButton.setOnClickListener(null)
                hint.visibility = View.VISIBLE
                motionActive = false
                hint.text = "Say \"Throw\""
                speechRecognizer.startListening(speechRecognizerIntent)

                // Stop recording after some time
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(
                        baseContext,
                        "Voice recording deactivated",
                        Toast.LENGTH_SHORT
                    ).show()

                    activeButton?.setBackgroundColor(Color.GRAY)
                    activeButton = null
                    hint.visibility = View.GONE
                    hint.text = ""
                    speechRecognizer.stopListening()
                }, 6000)
            }
        }

        // Set onClick action for activating the motion recognition
        motionButton.setOnClickListener {
            if (activeButton != motionButton || activeButton == null) {
                activeButton?.setBackgroundColor(Color.GRAY)
                motionButton.setBackgroundColor(Color.rgb(222, 147, 159))
                activeButton = motionButton
                speechRecognizer.stopListening()
                throwButton.visibility = View.GONE
                throwButton.setOnClickListener(null)
                hint.visibility = View.VISIBLE
                hint.text = "Shake your phone"
                motionActive = true
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        // Get recyclerView and present list data in it with adapter
        recyclerView = findViewById(R.id.recyclerview)
        // RecyclerView
        query?.let {
            adapter = object : RollAdapter(it) {
                override fun onDataChanged() {
                    // Hide content if the query returns empty.
                    if (itemCount == 0) {
                        //
                    } else {
                        //
                        recyclerView.smoothScrollToPosition(0)
                    }
                }

                override fun onError(e: FirebaseFirestoreException) {
                    Log.e("e", e.toString())
                }
            }
            recyclerView.adapter = adapter
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter?.startListening()
    }

    /**
     * Add a delay to the motion sensor so that the user cannot actively keep throwing the dice
     */
    private fun handleSensor() {
        motionActive = false
        sensorManager.unregisterListener(this, accelerometerSensor)
        Handler(Looper.getMainLooper()).postDelayed({
            if (accelerometerSensor != null && activeButton == motionButton) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                motionActive = true
            }
        }, 2000)
    }

    /**
     * Follow the sensor changes
     */
    override fun onSensorChanged(p0: SensorEvent) {
        if (p0.sensor.type == Sensor.TYPE_ACCELEROMETER && motionActive) {
            val x = p0.values[0]
            val y = p0.values[1]
            val z = p0.values[2]

            // Calculate the acceleration of the shake
            acLast = acCurrent
            acCurrent = sqrt((x * x + y * y + z * z))
            ac = ac * 1F + (acCurrent - acLast)
            if (ac > 10) {
                Log.d("throw", "throwing")
                throwDice()
                handleSensor()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not used
    }

    /**
     * User signs out
     */
    fun logout(view: View) {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Add and reduce the amount of dices
     * Change the appearance of the dices so that they fit on the screen
     */
    private fun handleDices(action : Int) {
        var newHeight: Int
        var newWidth: Int
        val params = diceImage1.layoutParams

        // Add dice
        if (action == 1) {
            dices++
            when (dices) {
                2 -> {
                    diceImage2.visibility = View.VISIBLE
                    chosenDice.add(diceImage2)

                    newWidth = 500
                    newHeight = 500
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                }
                3 -> {
                    diceImage3.visibility = View.VISIBLE
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    chosenDice.add(diceImage3)

                    newWidth = 330
                    newHeight = 330
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params
                }
                4 -> {
                    diceImage4.visibility = View.VISIBLE
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    if (!chosenDice.contains(diceImage3)) chosenDice.add(diceImage3)
                    chosenDice.add(diceImage4)

                    newWidth = 290
                    newHeight = 290
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params
                    diceImage4.layoutParams = params


                    val rowNewHeight = 300
                    val rowParams = row.layoutParams
                    rowParams.height = rowNewHeight

                    row.layoutParams = rowParams
                    findViewById<LinearLayout>(R.id.rowTwo).layoutParams = rowParams
                }
                5 -> {
                    diceImage5.visibility = View.VISIBLE
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    if (!chosenDice.contains(diceImage3)) chosenDice.add(diceImage3)
                    if (!chosenDice.contains(diceImage4)) chosenDice.add(diceImage4)
                    chosenDice.add(diceImage5)

                    newWidth = 290
                    newHeight = 290
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params
                    diceImage4.layoutParams = params
                    diceImage5.layoutParams = params
                }
                else -> {
                    diceImage6.visibility = View.VISIBLE
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    if (!chosenDice.contains(diceImage3)) chosenDice.add(diceImage3)
                    if (!chosenDice.contains(diceImage4)) chosenDice.add(diceImage4)
                    if (!chosenDice.contains(diceImage5)) chosenDice.add(diceImage5)
                    chosenDice.add(diceImage6)

                    newWidth = 290
                    newHeight = 290
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params
                    diceImage4.layoutParams = params
                    diceImage5.layoutParams = params
                    diceImage6.layoutParams = params
                }
            }
        }

        // Remove dice
        if (action == -1) {
            dices--
            when (dices) {
                1 -> {
                    diceImage2.visibility = View.GONE
                    if (!chosenDice.contains(diceImage1)) chosenDice.add(diceImage1)
                    chosenDice.remove(diceImage2)

                    newWidth = 500
                    newHeight = 500
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                }
                2 -> {
                    diceImage3.visibility = View.GONE
                    if (!chosenDice.contains(diceImage1)) chosenDice.add(diceImage1)
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    chosenDice.remove(diceImage3)

                    newWidth = 500
                    newHeight = 500
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                }
                3 -> {
                    diceImage4.visibility = View.GONE
                    if (!chosenDice.contains(diceImage1)) chosenDice.add(diceImage1)
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    if (!chosenDice.contains(diceImage3)) chosenDice.add(diceImage3)
                    chosenDice.remove(diceImage4)

                    newWidth = 330
                    newHeight = 330
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params

                    val rowNewHeight = 700
                    val rowParams = row.layoutParams
                    rowParams.height = rowNewHeight
                    row.layoutParams = rowParams
                }
                4 -> {
                    diceImage5.visibility = View.GONE
                    if (!chosenDice.contains(diceImage1)) chosenDice.add(diceImage1)
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    if (!chosenDice.contains(diceImage3)) chosenDice.add(diceImage3)
                    if (!chosenDice.contains(diceImage4)) chosenDice.add(diceImage4)
                    chosenDice.remove(diceImage5)

                    newWidth = 290
                    newHeight = 290
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params
                    diceImage4.layoutParams = params
                }
                else -> {
                    diceImage6.visibility = View.GONE
                    if (!chosenDice.contains(diceImage1)) chosenDice.add(diceImage1)
                    if (!chosenDice.contains(diceImage2)) chosenDice.add(diceImage2)
                    if (!chosenDice.contains(diceImage3)) chosenDice.add(diceImage3)
                    if (!chosenDice.contains(diceImage4)) chosenDice.add(diceImage4)
                    if (!chosenDice.contains(diceImage5)) chosenDice.add(diceImage5)
                    chosenDice.remove(diceImage6)

                    newWidth = 290
                    newHeight = 290
                    params.width = newWidth
                    params.height = newHeight

                    diceImage1.layoutParams = params
                    diceImage2.layoutParams = params
                    diceImage3.layoutParams = params
                    diceImage4.layoutParams = params
                    diceImage5.layoutParams = params
                }
            }
        }

        // If there is only one dice, it cannot be removed
        // If there is six dices, user cannot add more of them
        when (dices) {
            1 -> {
                reduceButton.isClickable = false
                reduceButton.setBackgroundColor(Color.GRAY)
            }
            6 -> {
                addButton.isClickable = false
                addButton.setBackgroundColor(Color.GRAY)
            }
            else -> {
                reduceButton.isClickable = true
                addButton.isClickable = true
                addButton.setBackgroundColor(Color.rgb(222, 147, 159))
                reduceButton.setBackgroundColor(Color.rgb(222, 147, 159))
            }
        }

        // Reset dices
        diceImage1.alpha = 1.0F
        diceImage2.alpha = 1.0F
        diceImage3.alpha = 1.0F
        diceImage4.alpha = 1.0F
        diceImage5.alpha = 1.0F
        diceImage6.alpha = 1.0F

        findViewById<TextView>(R.id.diceCount).text = dices.toString()
    }

    /**
     * Make dice rollable or unrollable
     * Dice that won't be rolled will become greyed out
     */
    private fun addDice(dice : ImageView) {
        if (!chosenDice.contains(dice)) {
            chosenDice.add(dice)
            dice.alpha = 1.0F
        }
        else if (chosenDice.size != 1){
            chosenDice.remove(dice)
            dice.alpha = 0.3F
        }
    }

    /**
     * Throw dices that the user has kept to be thrown
     */
    private fun throwDice() {
        val numbersList: ArrayList<Int> = ArrayList()
        for (dice: ImageView in chosenDice) {
            val roll = rollDice(dice)
            numbersList.add(roll)

        }
        saveRoll(numbersList)
    }

    /**
     * Get random number for a dice and change image of the dice
     */
    private fun rollDice(dice : ImageView) : Int {
        val roll = (1..6).random()
        when (roll) {
            1 -> dice.setImageResource(R.drawable.one)
            2 -> dice.setImageResource(R.drawable.two)
            3 -> dice.setImageResource(R.drawable.three)
            4 -> dice.setImageResource(R.drawable.four)
            5 -> dice.setImageResource(R.drawable.five)
            else -> dice.setImageResource(R.drawable.six)
        }
        return roll
    }

    /**
     * Add the rolled number into the firebase database
     */
    private fun saveRoll(num : ArrayList<Int>) {
        val time = getTime()
        val style = activeButton?.text.toString()
        val roll = Roll(time, num, style)
        db.collection(id)
            .add(roll)
            .addOnSuccessListener { documentReference ->
                Log.d("database", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("database", "Error adding document", e)
            }
    }

    /**
     * Get the current time
     */
    private fun getTime(): String {
        val time = Date()
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(time)
    }

    /**
     * Show statistics of the dice rolls
     */
    private fun showStatistics() {
        val stats = Statistics(db, id)
        stats.createPopUp(this, findViewById(R.id.parent))
    }


    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        sensorManager.unregisterListener(this)
    }

    /**
     * Check user permission for recording audio
     */
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }

    /**
     * Check if user grants permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission Granted",
                    Toast.LENGTH_SHORT
                ).show()
                micPermission = true
            }
        }
    }
}