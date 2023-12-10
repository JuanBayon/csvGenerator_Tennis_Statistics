package com.uoc.csvgenerator

import android.content.Context
import android.util.Log
import java.io.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class FirebaseData {
    data class SensorData(
        @SerializedName("x") val x: X,
        @SerializedName("y") val y: Y,
        @SerializedName("z") val z: Z,
        @SerializedName("timestamp") val timestamp: TimeStamp,
        @SerializedName("sensor") val sensor: Sensor,
        @SerializedName("sessionID") val sessionID: SessionID
    )

    data class SessionID(
        @SerializedName("integerValue") val integerValue: String
    )

    data class Sensor(
        @SerializedName("stringValue") val stringValue: String
    )

    data class TimeStamp(
        @SerializedName("stringValue") val stringValue: String
    )

    data class X(
        @SerializedName("doubleValue") val doubleValue: String
    )

    data class Y(
        @SerializedName("doubleValue") val doubleValue: String
    )

    data class Z(
        @SerializedName("doubleValue") val doubleValue: String
    )

    data class DatabaseRegister(
        @SerializedName("numEntries") val numEntries: NumEntries,
        @SerializedName("strokeType") val strokeType: StrokeType,
        @SerializedName("age") val age: Age,
        @SerializedName("backhand") val backhand: Backhand,
        @SerializedName("laterality") val lateratity: Laterality,
        @SerializedName("playerType") val playerType: PlayerType,
        @SerializedName("entries") val entries: Entries,
        @SerializedName("gender") val gender: Gender,
        )

    data class PlayerType(
        @SerializedName("stringValue") val stringValue: String
    )

    data class Age(
        @SerializedName("stringValue") val integerValue: String
    )

    data class NumEntries(
        @SerializedName("integerValue") val integerValue: String
    )

    data class Backhand(
        @SerializedName("stringValue") val stringValue: String
    )

    data class Laterality(
        @SerializedName("stringValue") val stringValue: String
    )

    data class Gender(
        @SerializedName("stringValue") val stringValue: String
    )

    data class StrokeType(
        @SerializedName("stringValue") val stringValue: String
    )

    data class Entries(
        @SerializedName("arrayValue") val arrayValue: ArrayValue
    )

    data class ArrayValue(
        @SerializedName("values") val values: List<Values>
    )

    data class Values (
        @SerializedName("mapValue") val mapValue: MapValue
    )

    data class MapValue(
        @SerializedName("fields") val fields: SensorData
    )

    data class Response(
        @SerializedName("documents") val documents: List<ResponseInside>
    )

    data class ResponseInside(
        @SerializedName("name") val name: String,
        @SerializedName("fields") val fields: DatabaseRegister
    )

    private lateinit var dataList: Response
    private fun asyncGetHttpRequest(
        onSuccess: (ApiResponse<Response>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = URL(BASE_URL)
            val openedConnection = url.openConnection() as HttpURLConnection
            openedConnection.requestMethod = "GET"

            val responseCode = openedConnection.responseCode
            try {
                val reader = BufferedReader(InputStreamReader(openedConnection.inputStream))
                val response = reader.readText()
                val apiResponse = ApiResponse(
                    responseCode,
                    parseJson<Response>(response)
                )
                reader.close()
                launch(Dispatchers.Main) {
                    onSuccess(apiResponse)
                }
            } catch (e: Exception) {
                Log.d("Error", e.message.toString())
                launch(Dispatchers.Main) {
                    onError(Exception("HTTP Request failed with response code $responseCode"))
                }
            } finally {

            }
        }
    }

    private inline fun <reified T>parseJson(text: String): T =
        Gson().fromJson(text, T::class.java)

    data class ApiResponse<T>(
        val responseCode: Int,
        val response: T
    )

    private fun transformToCsv(context: Context, csvName: String) {
        try {
            val file = openFile(csvName, context)
            val stream = FileOutputStream(file!!).apply {writeCsv(dataList)}
            stream.flush()
            stream.close()
        } catch (e: Exception){
            Log.w("CSV", "Error transforming to CSV, $e")
        }

    }
    private fun openFile(fileName: String?, context: Context): File? {
        var file: File? = null
        try {
            file = File(context.getExternalFilesDir(""), fileName!!)
        } catch (e: FileNotFoundException) {
            Log.w("CSV", "Error opening CSV, $e")
        }
        return file
    }

    private fun OutputStream.writeCsv(data: Response) {
        val writer = bufferedWriter()
        writer.write("""X, Y, Z, timestamp, sensor, sessionID, gender, strokeType, playerType, age, laterality, backhand""")
        writer.newLine()
        data.documents.forEach { list ->
            val gender = list.fields.gender.stringValue
            val age = list.fields.age.integerValue
            val laterality = list.fields.lateratity.stringValue
            val backhand = list.fields.backhand.stringValue
            val strokeType = list.fields.strokeType.stringValue
            val playerType = list.fields.playerType.stringValue
            list.fields.entries.arrayValue.values.forEach {
                val entry = it.mapValue.fields
                writer.write("${entry.x.doubleValue}, ${entry.y.doubleValue}, " +
                        "${entry.z.doubleValue}, ${entry.timestamp.stringValue}, " +
                        "${entry.sensor.stringValue}, ${entry.sessionID.integerValue}, " +
                        "${gender}, ${strokeType}, ${playerType}, ${age}, ${laterality}, ${backhand}")
                writer.newLine()
            }
        }
        writer.flush()
        writer.close()
    }

    fun getCSV(context: Context, csvName: String) {
        asyncGetHttpRequest(
            onSuccess = {
                dataList = it.response
                transformToCsv(context, csvName)
            },
            onError = {
                Log.d("ERROR", it.message.toString())
            }
        )
    }

    companion object Constant {
        private const val BASE_URL = "https://firestore.googleapis.com/v1/projects/tennis-42e1d/databases/(default)/documents/sensorData2"
    }
}