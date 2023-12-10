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
    private var csvName: String = "entries.csv"
    private var successText = "Done! CSV is saved in files"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        button = findViewById(R.id.button)
        button!!.setOnClickListener {
            val firebaseData = FirebaseData()
            firebaseData.getCSV(applicationContext, csvName)
            Toast.makeText(this, successText, Toast.LENGTH_SHORT).show()
        }

        exit = findViewById(R.id.exit)
        exit!!.setOnClickListener {
            finish()
        }

    }
}