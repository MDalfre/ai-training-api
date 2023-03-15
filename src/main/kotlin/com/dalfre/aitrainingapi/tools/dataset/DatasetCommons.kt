package com.dalfre.aitrainingapi.tools.dataset

import com.dalfre.aitrainingapi.model.PhraseSample
import com.dalfre.aitrainingapi.service.tokenization.Tokenizer
import org.springframework.stereotype.Component

import org.jetbrains.kotlinx.dl.dataset.Dataset
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.slf4j.LoggerFactory

@Component
class DatasetCommons(
    private val tokenizer: Tokenizer
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun buildDataset(
        toTrainModel: List<PhraseSample>,
        toTestModel: List<PhraseSample>
    ): Pair<Dataset, Dataset>{
        val preprocessedInputs = tokenizer.tokenizeList(toTrainModel.map { it.phrase })
        val preprocessedEvaluationInputs = tokenizer.tokenizeList(toTestModel.map { it.phrase })

        val dataset = OnHeapDataset.create(
            features = preprocessedInputs,
            labels = toTrainModel.map { it.feeling }.toFloatArray()
        )

        val evaluationDataset = OnHeapDataset.create(
            features = preprocessedEvaluationInputs,
            labels = toTestModel.map { it.feeling }.toFloatArray()
        )

        return Pair(dataset, evaluationDataset)
    }

    fun calculateSampleSize(dataset: Dataset) {
        logger.info("---------------------------------")
        logger.info("Dataset size: ${dataset.xSize()}")
        logger.info("Sample length: ${dataset.getX(0).size}")
        logger.info("Shape size: ${dataset.xSize()*dataset.getX(0).size}")
        logger.info("---------------------------------")
    }
}