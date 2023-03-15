package com.dalfre.aitrainingapi.model.response

import com.dalfre.aitrainingapi.model.FeelingEnum

data class PhrasePredictionResponse (
    val phrase: String,
    val feeling: FeelingEnum,
    val measure: Float
)