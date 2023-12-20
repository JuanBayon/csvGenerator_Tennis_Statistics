package com.uoc.csvgenerator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.uoc.csvgenerator.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var button: Button? = null
    private var exit: Button? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        button = findViewById(R.id.button)
        button!!.setOnClickListener {
            val firebaseData = FirebaseData()
            firebaseData.getCSV(applicationContext, CSV_NAME)
            Toast.makeText(this, SUCCESS_TEXT, Toast.LENGTH_SHORT).show()
        }

        exit = findViewById(R.id.exit)
        exit!!.setOnClickListener {
            finish()
        }

    }

    companion object Constants {
        const val CSV_NAME: String = "entries.csv"
        const val SUCCESS_TEXT = "Done! CSV is saved in files"
    }
}