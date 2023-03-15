package com.dalfre.aitrainingapi.service

import com.dalfre.aitrainingapi.model.FeelingEnum
import com.dalfre.aitrainingapi.model.PhraseSample
import com.dalfre.aitrainingapi.model.PhraseTagEnum
import com.dalfre.aitrainingapi.repository.PhraseSampleRepository
import com.dalfre.aitrainingapi.samples.Samples
import com.dalfre.aitrainingapi.service.tokenization.Tokenizer
import com.dalfre.aitrainingapi.tools.ModelUtils
import com.dalfre.aitrainingapi.tools.dataset.DatasetCommons
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.WritingMode
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adamax
import org.jetbrains.kotlinx.dl.api.summary.printSummary
import org.jetbrains.kotlinx.dl.dataset.evaluate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class AiTraining(
    private val datasetCommons: DatasetCommons,
    private val tokenizer: Tokenizer,
    private val phraseSampleRepository: PhraseSampleRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun trainWithDatabase(epochs: Int) {
        val loadedPhrases = phraseSampleRepository.findAll()
        val phrasesToTrain = mutableListOf<PhraseSample>()
        val phrasesToTest = mutableListOf<PhraseSample>()

        if (loadedPhrases.isEmpty()) {
            logger.warn("Initializing phrase database population ...")

            Samples.phrasesToTrain.forEach { phrasesToTrain.add(PhraseSample(it, PhraseTagEnum.TRAINING)) }
            Samples.phraseTest.forEach { phrasesToTest.add(PhraseSample(it, PhraseTagEnum.SAMPLE)) }

            phraseSampleRepository.saveAll(phrasesToTrain.plus(phrasesToTest))
        } else {
            loadedPhrases.filter { it.tag == PhraseTagEnum.TRAINING }.shuffled()
            loadedPhrases.filter { it.tag == PhraseTagEnum.SAMPLE }.shuffled()
        }

        if (tokenizer.getDictionary() == null) {
            logger.warn("Initializing dictionary ... ")
            tokenizer.createDictionary(phrasesToTrain.plus(phrasesToTest).map { it.phrase })
        }

        trainAi(phrasesToTrain = phrasesToTrain, phrasesToTest = phrasesToTest, epochs = epochs)
    }

    fun trainAi(
        phrasesToTrain: List<PhraseSample>,
        phrasesToTest: List<PhraseSample>,
        epochs: Int
    ) {
        logger.info("Starting training ...")

        val maximumLength = phrasesToTrain.maxBy { it.phrase.split(" ").size }.phrase.split(" ").size

        val model = Sequential.of(
            Input(maximumLength.toLong(), name = "InputTruth"),
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

        val (dataset, evaluationDataset) = datasetCommons.buildDataset(phrasesToTrain, phrasesToTest)

        datasetCommons.calculateSampleSize(dataset)

        model.use { compiledModel ->

            if(File("model/my_model").exists()) {
                model.loadWeights(File("model/my_model"))
            }

            compiledModel.fit(trainingDataset = dataset, validationDataset = evaluationDataset, epochs = epochs)
            val accuracy = compiledModel.evaluate(dataset, Metrics.ACCURACY)

            logger.info("Nivel de confiança: $accuracy")

            compiledModel.printSummary()
            compiledModel.save(File("model/my_model"), writingMode = WritingMode.OVERRIDE)

            val predictedModel = mutableListOf<Pair<String, Float>>()

            phrasesToTest.forEach {
                val prediction = compiledModel.predictSoftly(tokenizer.tokenizeString(it.phrase, maximumLength))

                val result =
                    if (prediction.first() > 0.0f) {
                        predictedModel.add(Pair(it.phrase, 1.0f))
                        """
                            This message: ${it.phrase}
                            └─ Feeling: ${FeelingEnum.POSITIVE}	Measure: ${prediction.first()}
                        """.trimIndent()
                    } else {
                        predictedModel.add(Pair(it.phrase, 0.0f))
                        """
                            This message: ${it.phrase}
                            └─ Feeling: ${FeelingEnum.NEGATIVE} Measure: ${prediction.first()}
                        """.trimIndent()
                    }
                logger.info(result)
            }
            logger.info("Accuracy: ${ModelUtils.checkAccuracy(phrasesToTest, predictedModel)}")
        }
    }

}