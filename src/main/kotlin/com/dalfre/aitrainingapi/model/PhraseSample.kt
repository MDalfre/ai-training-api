package com.dalfre.aitrainingapi.model

data class PhraseSample(
    val phrase: String,
    val feeling: Float,
    val tag: PhraseTagEnum
) {
    constructor(pair: Pair<String, Float>, phraseTag: PhraseTagEnum) : this(
        phrase = pair.first,
        feeling = pair.second,
        tag = phraseTag
    )
}
