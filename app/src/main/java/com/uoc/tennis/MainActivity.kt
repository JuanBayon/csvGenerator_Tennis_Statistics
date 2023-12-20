package com.uoc.tennis

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.uoc.tennis.databinding.ActivityMainBinding
import android.hardware.SensorEventListener
import android.util.Log
import android.view.View.OnClickListener
import android.widget.TextView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.firebase.firestore.FirebaseFirestore
import com.uoc.tennis.AI.Model
import com.uoc.tennis.AI.StrokeTypes
import com.uoc.tennis.AI.TransformSensorData
import java.lang.Exception
import java.time.Instant
import java.time.format.DateTimeFormatter

class MainActivity : Activity(), SensorEventListener, OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private val dataArray: ArrayList<SensorData> = java.util.ArrayList()
    private var dataAnalyzed: ArrayList<StrokeTypes>? = ArrayList()
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null
    //private var mMagnetic: Sensor? = null
    private var button: Button? = null
    private var title: TextView? = null
    private var text: TextView? = null
    private var sessionInfo: TextView? = null
    private var clicks: Int = INITIAL_CLICK
    private var sessionID: Int? = null
    private var numberDatum: Int = INITIAL_DATUM
    private var playerType: String = NONE
    private var hitType: String = NONE
    private var gender: String = NONE
    private var age: String = NONE
    private var backhand: String = NONE
    private var laterality: String = NONE
    private var textTitle: TextView? = null

    private lateinit var firebase: FirebaseFirestore
    private val jacksonMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    data class SensorData(
        val x: Double, val y: Double, val z: Double,
        val timestamp: String, val sensor: String, val sessionID: Int)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionID = (SESSION_ID_MIN..SESSION_ID_MAX).shuffled().first()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroscope = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        //mMagnetic = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        firebase = FirebaseFirestore.getInstance()

        button = findViewById(R.id.button)
        title = findViewById(R.id.textTitle)
        text = findViewById(R.id.text)
        textTitle = findViewById(R.id.textTitle)
        sessionInfo = findViewById(R.id.sessionInfo)

        button!!.setOnClickListener(this)
        sessionInfo!!.setOnClickListener {
            val intent = Intent(this, ConfigurationMenuActivity::class.java)
            startActivityForResult(intent, CONFIGURATION)
        }
    }


    override fun onClick(v: View){
        clicks += 1
        when (clicks) {
            FIRST_CLICK -> {
                sessionInfo!!.visibility = View.GONE
                button!!.setText(R.string.buttonStop)
                text!!.setText(R.string.stopText)
                title!!.setText(R.string.stopTitle)
                mSensorManager!!.registerListener( this, mAccelerometer!!, SensorManager.SENSOR_DELAY_GAME)
                mSensorManager!!.registerListener(this, mGyroscope!!, SensorManager.SENSOR_DELAY_GAME)
                //mSensorManager!!.registerListener(this, mMagnetic!!, SensorManager.SENSOR_DELAY_GAME)
            }
            SECOND_CLICK -> {
                button!!.setText(R.string.stoppedButton)
                text!!.setText(R.string.stoppedText)
                title!!.setText(R.string.stoppedTitle)
                mSensorManager!!.unregisterListener(this)
                dataAnalyzed = analyzeData()
                saveInDatabase()
                button!!.visibility = View.GONE
                sessionInfo!!.text = EXIT
                sessionInfo!!.visibility = View.VISIBLE
                sessionInfo!!.setOnClickListener {
                    finish()
                }
                val smashNumber = dataAnalyzed!!.count { x -> x == StrokeTypes.SMASH }
                val driveNumber = dataAnalyzed!!.count { x -> x == StrokeTypes.DRIVE }
                val backhandNumber = dataAnalyzed!!.count { x -> x == StrokeTypes.BACKHAND }
                val serviceNumber = dataAnalyzed!!.count { x -> x == StrokeTypes.SERVICE }
                val vrNumber = dataAnalyzed!!.count { x -> x == StrokeTypes.VR }
                val vlNumber = dataAnalyzed!!.count { x -> x == StrokeTypes.VL }
                textTitle!!.text = "${SMASH}: ${smashNumber}\n ${DRIVE}: ${driveNumber}\n ${BACKHAND}: ${backhandNumber}\n" +
                        "${SERVICE}: ${serviceNumber}\n ${VR}: ${vrNumber}\n ${VL}: ${vlNumber}\n"
            }
            //TODO SHOW THE RESULTS IN THE A NEW ACTIVITY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val datum = SensorData(event.values[X_COORDINATE].toDouble(), event.values[Y_COORDINATE].toDouble(),
            event.values[Z_COORDINATE].toDouble(), DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            event.sensor.name, sessionID!!)
        dataArray.add(datum)
        numberDatum++
        if ( numberDatum % MAX_DATUM_TO_SEND == ZERO) {
            saveInDatabase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        playerType =  data!!.getStringExtra(PLAYER_TYPE)!!
        hitType =  data.getStringExtra(STROKE_TYPE)!!
        gender = data.getStringExtra(GENDER)!!
        age = data.getStringExtra(AGE)!!
        laterality = data.getStringExtra(LATERALITY)!!
        backhand = data.getStringExtra(BACKHAND)!!
        Log.i(getString(R.string.config), getString(R.string.data))
    }

    /**
     * The function returns the list of recognized strokes, from the list captured by the sensors.
     */
    private fun analyzeData(): ArrayList<StrokeTypes>? {
        try {
            val result: ArrayList<StrokeTypes> = ArrayList()
            val td = TransformSensorData(dataArray, gender, age, laterality, backhand, hitType, playerType)
            val mapList = td.getStatisticalData()
            for (i in mapList.size -1  downTo 0) {
                result.add(Model.getPrediction(mapList.elementAt(i), this))
            }
            // TEST EXAMPLE FOR THE PREDICTION MODEL
            //val list = getExample()
            //val pred = Model.getPrediction(list, this)
            //result.add(pred)
            return result
            Log.i(getString(R.string.analyze), getString(R.string.data))
        } catch (e: Exception){
            Log.e(getString(R.string.error), getString(R.string.data_error) + e.message)
        }
        return null
    }

    /**
     * Saves in FireStore all the data present in the list of SensorData and the attributes of the session.
     */
    private fun saveInDatabase() {
        try {
            val hashMap = hashMapOf(
                ENTRIES to dataArray.map { jacksonMapper.convertValue(it, Map::class.java) },
                PLAYER_TYPE to playerType,
                STROKE_TYPE to hitType,
                GENDER to gender,
                NUM_ENTRIES to numberDatum,
                LATERALITY to laterality,
                BACKHAND to backhand,
                AGE to age
            )
            dataArray.clear()
            numberDatum = INITIAL_DATUM
            firebase.collection(COLLECTION).add(hashMap)
            Log.i(getString(R.string.firebase), getString(R.string.data))
        } catch (e: Exception){
            Log.e(getString(R.string.error), getString(R.string.data_error) + e.message)
        }
    }

    /**
     * Provides an example with all the fields to be used to predict with the model.
     */
    private fun getExample(): HashMap<String, Any> {
        val result =  HashMap<String, Any>()
        result["x_mean_acc"] = 0.119
        result["y_mean_acc"] = 0.075
        result["z_mean_acc"] = -0.011
        result["x_std_acc"] = 0.056
        result["y_std_acc"] = 0.036
        result["z_std_acc"] = 0.011
        result["x_min_acc"] = -0.069
        result["y_min_acc"] = -0.069
        result["z_min_acc"] = -0.03
        result["x_max_acc"] = 0.33
        result["y_max_acc"] = 0.2
        result["z_max_acc"] = 0.07
        result["x_median_acc"] = 0.13
        result["y_median_acc"] = 0.08
        result["z_median_acc"] = -0.011
        result["x_mad_acc"] = 0.037
        result["y_mad_acc"] = 0.025
        result["z_mad_acc"] = 0.01
        result["x_iqr_acc"] = 0.07
        result["y_iqr_acc"] = 0.05
        result["z_iqr_acc"] = 0.02
        result["x_pcount_acc"] = 127
        result["y_pcount_acc"] = 127
        result["z_pcount_acc"] = 27
        result["x_ncount_acc"] = 3
        result["y_ncount_acc"] = 3
        result["z_ncount_acc"] = 103
        result["x_abvmean_acc"] = 75
        result["y_abvmean_acc"] = 74
        result["z_abvmean_acc"] = 65
        result["x_skew_acc"] = -0.55
        result["y_skew_acc"] = -0.607
        result["z_skew_acc"] = -0.09
        result["x_kurt_acc"] = 2.9
        result["y_kurt_acc"] = 2.17
        result["z_kurt_acc"] = -1.53
        result["time_acc"] = 2600
        result["x_energy_acc"] = 0.022
        result["y_energy_acc"] = 0.009
        result["z_energy_acc"] = 0.0003

        result["x_mean_gyr"] = -0.34
        result["y_mean_gyr"] = -0.28
        result["z_mean_gyr"] = 0.029
        result["x_std_gyr"] = 0.28
        result["y_std_gyr"] = 0.19
        result["z_std_gyr"] = 0.08
        result["x_min_gyr"] = -0.98
        result["y_min_gyr"] = -0.88
        result["z_min_gyr"] = -0.25
        result["x_max_gyr"] = 0.004
        result["y_max_gyr"] = 0.004
        result["z_max_gyr"] = 0.38
        result["x_median_gyr"] = -0.31
        result["y_median_gyr"] = -0.31
        result["z_median_gyr"] = 0.013
        result["x_mad_gyr"] = 0.23
        result["y_mad_gyr"] = 0.14
        result["z_mad_gyr"] = 0.019
        result["x_iqr_gyr"] = 0.48
        result["y_iqr_gyr"] = 0.25
        result["z_iqr_gyr"] = 0.05
        result["x_pcount_gyr"] = 25
        result["y_pcount_gyr"] = 28
        result["z_pcount_gyr"] = 186
        result["x_ncount_gyr"] = 235
        result["y_ncount_gyr"] = 230
        result["z_ncount_gyr"] = 73
        result["x_abvmean_gyr"] = 144
        result["y_abvmean_gyr"] = 120
        result["z_abvmean_gyr"] = 104
        result["x_skew_gyr"] = -0.53
        result["y_skew_gyr"] = -0.10
        result["z_skew_gyr"] = 0.2
        result["x_kurt_gyr"] = -0.76
        result["y_kurt_gyr"] = -0.56
        result["z_kurt_gyr"] = 3.49
        result["time_gyr"] = 2630

        result["player"] = 0.0
        result["age"] = 27.0
        result["level"] = 1.0
        result["backhand"] = 2.0

        return result
    }

    companion object Constants {
        const val COLLECTION = "sensorData2"
        const val ENTRIES = "entries"
        const val NUM_ENTRIES = "numEntries"
        const val FIRST_LABEL = "first"
        const val SECOND_LABEL = "second"
        const val THIRD_LABEL = "third"
        const val FIRST_OPTION = "first_option"
        const val SECOND_OPTION = "second_option"
        const val THIRD_OPTION = "third_option"
        const val LATERALITY = "laterality"
        const val BACKHAND = "backhand"
        const val AGE = "age"
        const val PLAYER_TYPE = "playerType"
        const val STROKE_TYPE = "strokeType"
        const val GENDER = "gender"
        const val NONE = "none"
        const val OPTION = "option"
        const val OPTION_NAME = "optionName"
        const val MALE = "male"
        const val FEMALE = "female"
        const val OTHER = "other"
        const val DRIVE = "drive"
        const val BALL = "ball"
        const val CASUAL = "casual"
        const val AMATEUR = "amateur"
        const val PROFESSIONAL = "professional"
        const val RIGHT = "right"
        const val LEFT = "left"
        const val ONE = "one"
        const val TWO = "two"
        const val EXIT = "Exit"
        const val SMASH = "smash"
        const val SERVICE = "service"
        const val VR = "vd"
        const val VL = "vl"
        const val CONFIGURATION = 1
        const val CONFIGURATION_MENU = 2
        const val INITIAL_DATUM = 0
        const val INITIAL_CLICK = 0
        const val MAX_DATUM_TO_SEND = 10000
        const val SESSION_ID_MIN = 100000
        const val SESSION_ID_MAX = 999999
        const val FIRST_CLICK = 1
        const val SECOND_CLICK = 2
        const val X_COORDINATE = 0
        const val Y_COORDINATE = 1
        const val Z_COORDINATE = 2
        const val ZERO = 0
    }

}