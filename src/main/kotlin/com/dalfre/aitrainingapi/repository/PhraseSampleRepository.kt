package com.dalfre.aitrainingapi.repository

import com.dalfre.aitrainingapi.model.PhraseSample
import org.springframework.data.mongodb.repository.MongoRepository

interface PhraseSampleRepository : MongoRepository<PhraseSample, String>