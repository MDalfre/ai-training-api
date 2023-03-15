package com.dalfre.aitrainingapi.repository

import com.dalfre.aitrainingapi.model.Dictionary
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DictionaryRepository: MongoRepository<Dictionary, Double> {

    fun findFirstByOrderByIdDesc(): Optional<Dictionary>
}