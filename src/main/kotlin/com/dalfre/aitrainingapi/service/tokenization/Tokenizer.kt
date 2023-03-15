package com.dalfre.aitrainingapi.service.tokenization

import com.dalfre.aitrainingapi.model.Dictionary
import com.dalfre.aitrainingapi.repository.DictionaryRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Component
class Tokenizer(
    private val dictionaryRepository: DictionaryRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val appender = "*"
    private val newWords = mutableMapOf<String, Float>()

    fun createDictionary(phrases: List<String>): MutableMap<String, Float> {
//        dictionaryRepository.findFirstByOrderByIdDesc()
        val rawWords = hashSetOf<String>()
        val dictionary = mutableMapOf<String, Float>()
        phrases.forEach {
            it.lowercase().let { formattedPhrase ->
                formattedPhrase.split(" ").forEach { word ->
                    rawWords.add(removePunctuation(word))
                }
            }
        }
        dictionary[appender] = 0.00f
        rawWords.indices.forEach {
            dictionary[rawWords.elementAt(it)] = (it.plus(1).toFloat() / 1000).plus(1)
        }

        logger.info("Found ${dictionary.size} words in dictionary")
        updateDictionary(dictionary)
        logger.info(dictionary.toString())
        return dictionary
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getDictionary(): Map<String, Float>? {
        return dictionaryRepository.findFirstByOrderByIdDesc().getOrNull()?.dictionary
    }

    fun tokenizeList(input: List<String>, log: Boolean = false): Array<FloatArray> {

        val dictionary = dictionaryRepository.findFirstByOrderByIdDesc().orElseThrow().dictionary

        if (log) logger.info("----DATA as Float----")

        val maximumLength = input.maxBy { it.split(" ").size }.split(" ").size

        val response = Array(size = input.size) {
            val array = input[it].let { phrase ->
                val dictionaryEntry = mutableListOf<Float>()
                phrase.lowercase().split(" ").forEach { word ->
                    dictionaryEntry.add(
                        dictionary[removePunctuation(word)] ?: updateNewWord(word, dictionary)
                    )
                }
                while (dictionaryEntry.size < maximumLength) {
                    dictionaryEntry.add(dictionary[appender]!!)
                }
                dictionaryEntry
            }
            if (log) logger.info(array.toString())
            array.toFloatArray()
        }
        commitDictionary(dictionary)
        return response.also { if (log) logger.info("----END DATA----") }
    }

    fun tokenizeString(input: String, maxLength: Int): FloatArray {

        val dictionary = dictionaryRepository.findFirstByOrderByIdDesc().orElseThrow().dictionary
        val dictionaryEntry = mutableListOf<Float>()

        input.lowercase().split(" ").forEach { word ->
            dictionaryEntry.add(
                dictionary[removePunctuation(word)] ?: updateNewWord(word, dictionary)
            )
        }
        while (dictionaryEntry.size < maxLength) {
            dictionaryEntry.add(dictionary[appender]!!)
        }
        commitDictionary(dictionary)
        return dictionaryEntry.toFloatArray()
    }

    private fun removePunctuation(word: String): String {
        val grammarPunctuation = listOf('.', '!', '?', ',')
        return if (word.last() in grammarPunctuation) {
            word.substringBefore(word.last())
        } else {
            word
        }
    }

    private fun updateDictionary(dictionary: Map<String, Float>) {
        val createdDictionary = Dictionary(dictionary.size, dictionary)
        try {
            dictionaryRepository.save(createdDictionary)
        } catch (ex: DuplicateKeyException) {
            println(ex::class.java)
            logger.info("Dictionary is already updated at version ${dictionary.size.toDouble() / 1000}")
        }

    }

    private fun updateNewWord(word: String, currentDictionary: Map<String, Float>): Float {
        val newWordValue = currentDictionary.maxBy { it.value }.value
            .plus(newWords.size.plus(1) / 1000)

        newWords[word] = newWordValue

        logger.info("New word found: $word - $newWordValue")

        return newWordValue
    }

    private fun commitDictionary(currentDictionary: Map<String, Float>) {
        if (newWords.isNotEmpty()) {
            val newDictionary = currentDictionary.plus(newWords)
            dictionaryRepository.save(Dictionary(newDictionary.size, newDictionary))
            logger.info("Dictionary updated to version: ${newDictionary.size.toDouble() / 1000}")
            newWords.clear()
        }
    }
}