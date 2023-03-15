package com.dalfre.aitrainingapi.service

import com.dalfre.aitrainingapi.model.FeelingEnum
import com.dalfre.aitrainingapi.model.response.PhrasePredictionResponse
import com.dalfre.aitrainingapi.service.tokenization.Tokenizer
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adamax
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class AiPrediction(
    private val tokenizer: Tokenizer
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun predictWithModel(phrase: String): PhrasePredictionResponse {

        val phraseSize = phrase.split(" ").size
        val tokenizedPhrase = tokenizer.tokenizeString(phrase, phraseSize)

        val model = Sequential.of(
            Input(phraseSize.toLong(), name = "InputTruth"),
            Dense(256, activation = Activations.Elu, useBias = true, name = "dense_2"),
            Dense(128, activation = Activations.Relu, useBias = true, name = "dense_1"),
            Dense(128, activation = Activations.Relu, useBias = true, name = "dense_3"),
            Dense(16, activation = Activations.Sigmoid, useBias = true, name = "dense_4"),
            Dense(1, activation = Activations.Tanh, useBias = true, name = "dense_5")
        )

        model.compile(
            optimizer = Adamax(),
            loss = Losses.BINARY_CROSSENTROPY, //SQUARED_HINGE
            metric = Metrics.ACCURACY
        )

        model.use {
            it.loadWeights(File("model/my_model"),true)
            val prediction = it.predictSoftly(tokenizedPhrase)

            val result =
                if (prediction.first() > 0.0f) {
                    logger.info(
                    """
                     This message: $phrase
                     └─ Feeling: ${FeelingEnum.POSITIVE} Measure: ${prediction.first()}
                     """.trimIndent())
                    PhrasePredictionResponse(phrase, FeelingEnum.POSITIVE, prediction.first())
                } else {
                    logger.info(
                    """
                     This message: $phrase
                     └─ Feeling: ${FeelingEnum.NEGATIVE} Measure: ${prediction.first()}
                     """.trimIndent())
                    PhrasePredictionResponse(phrase, FeelingEnum.NEGATIVE, prediction.first())
                }
            return result
        }
    }
}