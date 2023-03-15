package com.dalfre.aitrainingapi.model

import org.springframework.data.annotation.Id

data class Dictionary(
    @Id
    val id: Double,
    val dictionary: Map<String, Float>
) {
    constructor(id: Int, dictionary: Map<String, Float>): this(id = id.toDouble() / 1000, dictionary)
}
