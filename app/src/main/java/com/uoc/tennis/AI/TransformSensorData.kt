package com.uoc.tennis.AI

import com.uoc.tennis.MainActivity
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math.stat.descriptive.moment.Kurtosis
import org.apache.commons.math.stat.descriptive.moment.Skewness
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math.stat.descriptive.rank.Median
import java.text.SimpleDateFormat
import java.time.Period
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.streams.toList


class TransformSensorData(
    data: ArrayList<MainActivity.SensorData>,
    gender: String,
    age: String,
    laterality: String,
    backhand: String,
    hitType: String,
    playerType: String
) {

    private var list: ArrayList<MainActivity.SensorData> = data
    private var gender: String = gender
    private var age: String = age
    private var laterality: String = laterality
    private var backhand: String = backhand
    private var strokeType: String = hitType
    private var playerType: String = playerType
    private var acc_list_parts: ArrayList<ArrayList<MainActivity.SensorData>>? = null
    private var gyr_list_parts: ArrayList<ArrayList<MainActivity.SensorData>>? = null

    /**
     * The function is used to slice, calculate all the statistical data and return the dataset.
     */
    fun getStatisticalData(): ArrayList<HashMap<String, Any>> {
        val result = ArrayList<HashMap<String, Any>>()

        val iterations = min(acc_list_parts!!.size, gyr_list_parts!!.size)
        for (i in iterations-1  downTo 0 step 1) {
            sliceData()
            val resultMap = statisticalDataForOneStroke(acc_list_parts!![i], gyr_list_parts!![i])
            result.add(resultMap)
        }

        return result
    }

    /**
     * DIVIDE THE LIST IN PARTS. IT WAS DISCOVERED THAT EACH STROKE HAVE AROUND 130 ACCELEROMOTER ENTRIES AND 260 GYROSCOPE ENTRIES.
     * The function receives no argument, and creates the parts stored in the attributes of the class.
     */
    private fun sliceData() {
        val acc_list: List<MainActivity.SensorData> = list.stream().filter{ a -> a.sensor == "Accelerometer" }.toList()
        val gyr_list: List<MainActivity.SensorData> = list.stream().filter{ a -> a.sensor == "Gyroscope" }.toList()

        val acc_parts = ceil((acc_list.size.toDouble() / 130)).toInt()
        acc_list_parts = ArrayList()
        var index = 0
        for (i in 1..acc_parts) {
            if (i != acc_parts) {
                acc_list_parts!!.add(list.subList(index, 130*i) as ArrayList<MainActivity.SensorData>)

            } else {
                acc_list_parts!!.add(list.subList(index, acc_parts) as ArrayList<MainActivity.SensorData>)
            }
            index = 130*i + 1
        }

        val gyr_parts = ceil((gyr_list.size.toDouble() / 260)).toInt()
        gyr_list_parts = ArrayList()
        index = 0
        for (i in 1..gyr_parts) {
            if (i != gyr_parts) {
                gyr_list_parts!!.add(
                    list.subList(
                        index,
                        260 * i
                    ) as ArrayList<MainActivity.SensorData>
                )
            } else {
                gyr_list_parts!!.add(
                    list.subList(
                        index,
                        gyr_parts
                    ) as ArrayList<MainActivity.SensorData>
                )
            }
            index = 130 * i + 1
        }
    }

    private fun statisticalDataForOneStroke(acc_list:List<MainActivity.SensorData> , gyr_list: List<MainActivity.SensorData>): HashMap<String, Any> {
        val resultMap = HashMap<String, Any>()
        val xs_acc = acc_list.stream().mapToDouble { a -> a.x}.toList()
        val ys_acc = acc_list.stream().mapToDouble { a -> a.y}.toList()
        val zs_acc = acc_list.stream().mapToDouble { a -> a.z}.toList()

        val xs_gyr = gyr_list.stream().mapToDouble { a -> a.x}.toList()
        val ys_gyr = gyr_list.stream().mapToDouble { a -> a.y}.toList()
        val zs_gyr = gyr_list.stream().mapToDouble { a -> a.z}.toList()

        resultMap["sessionID"] = list[0].sessionID
        val period_acc = Period.between(SimpleDateFormat("dd-MM-yyyy").parse(acc_list.elementAt(-1).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
            SimpleDateFormat("dd-MM-yyyy").parse(acc_list.elementAt(0).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        resultMap["time_acc"] = period_acc.days * 3600 * 24

        val period_gyr = Period.between(SimpleDateFormat("dd-MM-yyyy").parse(gyr_list.elementAt(-1).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
            SimpleDateFormat("dd-MM-yyyy").parse(gyr_list.elementAt(0).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        resultMap["time_gyr"] = period_gyr.days * 3600 * 24

        calculateStatistics(resultMap, xs_acc, "acc", "x")
        calculateStatistics(resultMap, ys_acc, "acc", "y")
        calculateStatistics(resultMap, zs_acc, "acc", "z")

        calculateStatistics(resultMap, xs_gyr, "gyr", "x")
        calculateStatistics(resultMap, ys_gyr, "gyr", "y")
        calculateStatistics(resultMap, zs_gyr, "gyr", "z")

        resultMap[MainActivity.AGE] = age
        resultMap[MainActivity.GENDER] = gender
        resultMap[MainActivity.STROKE_TYPE] = strokeType
        resultMap[MainActivity.PLAYER_TYPE] = playerType
        resultMap[MainActivity.LATERALITY] = laterality
        resultMap[MainActivity.BACKHAND] = backhand

        return resultMap
    }

    /**
     * Calculate statistics realated to one axis and one sensor, passed as arguments. The list of data and HashMap where to
     * store the results are also passed to the function. The function returns the HashMap modified.
     */
    private fun calculateStatistics(resultMap: HashMap<String, Any>, data: List<Double>, suffix: String, prefix: String): HashMap<String, Any> {

        val average = data.average()
        resultMap["${prefix}_mean_${suffix}"] = average
        val sd = StandardDeviation()
        resultMap["${prefix}_std_${suffix}"] = sd.evaluate(data.toDoubleArray())
        resultMap["${prefix}_min_${suffix}"] = data.min()
        resultMap["${prefix}_max_${suffix}"] = data.max()
        val median = Median()
        resultMap["${prefix}_median_${suffix}"] = median.evaluate(data.toDoubleArray())
        resultMap["${prefix}_mad_${suffix}"] = median.evaluate(data.map { x -> abs(x - median.evaluate(x)) }.toDoubleArray())
        val da = DescriptiveStatistics(data.toDoubleArray())
        resultMap["${prefix}_iqr_${suffix}"] = da.getPercentile(75.0) - da.getPercentile(25.0)
        resultMap["${prefix}_pcount_${suffix}"] = data.filter { x -> x > 0 }.sum()
        resultMap["${prefix}_ncount_${suffix}"] = data.filter { x -> x < 0 }.sum()
        resultMap["${prefix}_abvmean_${suffix}"] = data.filter { x -> x > average}.sum()
        val sk = Skewness()
        resultMap["${prefix}_skew_${suffix}"] = sk.evaluate(data.toDoubleArray())
        val krt = Kurtosis()
        resultMap["${prefix}_kurt_${suffix}"] = krt.evaluate(data.toDoubleArray())
        if (suffix == "acc") {
            resultMap["${prefix}_energy_${suffix}"] = data.map { x -> x.pow(2.0) }.sum() / 100
        }
        return resultMap
    }


}