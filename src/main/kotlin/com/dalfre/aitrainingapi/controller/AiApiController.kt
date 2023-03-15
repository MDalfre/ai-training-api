package com.dalfre.aitrainingapi.controller

import com.dalfre.aitrainingapi.model.PhraseSample
import com.dalfre.aitrainingapi.model.PhraseTagEnum
import com.dalfre.aitrainingapi.model.request.PredictionRequest
import com.dalfre.aitrainingapi.model.response.PhrasePredictionResponse
import com.dalfre.aitrainingapi.repository.PhraseSampleRepository
import com.dalfre.aitrainingapi.samples.Samples
import com.dalfre.aitrainingapi.service.AiPrediction
import com.dalfre.aitrainingapi.service.AiTraining
import com.dalfre.aitrainingapi.service.tokenization.Tokenizer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AiApiController(
    private val aiTraining: AiTraining,
    private val aiPrediction: AiPrediction,
    private val tokenizer: Tokenizer,
    private val phraseSampleRepository: PhraseSampleRepository
) {

    @GetMapping("/load_phrases")
    fun loadPhrases(): MutableList<PhraseSample> {
        val trainingPhraseSample = mutableListOf<PhraseSample>()
        val phraseSamples = mutableListOf<PhraseSample>()

        Samples.phrasesToTrain.forEach { trainingPhraseSample.add(PhraseSample(it, PhraseTagEnum.TRAINING)) }
        Samples.phraseTest.forEach { phraseSamples.add(PhraseSample(it, PhraseTagEnum.SAMPLE)) }

        return phraseSampleRepository.saveAll(phraseSamples)
    }

    @GetMapping("/dictionary")
    fun createDictionary(): MutableMap<String, Float> {
        val phrase = phraseSampleRepository.findAll()
        return tokenizer.createDictionary(phrase.map { it.phrase })
    }

    @GetMapping("/train")
    fun trainModel(): String {
        aiTraining.trainWithDatabase(10000)
        return "Training in process..."
    }

    @PostMapping("/preditc")
    fun predict(@RequestBody phrase: PredictionRequest): PhrasePredictionResponse {
        return aiPrediction.predictWithModel(phrase.phrase)
    }

}