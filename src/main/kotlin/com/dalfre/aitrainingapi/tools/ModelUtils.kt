package com.dalfre.aitrainingapi.tools

import com.dalfre.aitrainingapi.model.PhraseSample

object ModelUtils {

    fun checkAccuracy(sampleModel: List<PhraseSample>, predictedModel: List<Pair<String, Float>>): Float {
        var assertionsCounter = 0
        predictedModel.forEach {predict ->
            val sample = sampleModel.find { it.phrase == predict.first}
            if (sample?.feeling == predict.second) {
                assertionsCounter++
            }
        }

        return assertionsCounter.toFloat()/predictedModel.size.toFloat()
    }
}