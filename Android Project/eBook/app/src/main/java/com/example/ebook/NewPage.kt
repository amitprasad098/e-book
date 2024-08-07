package com.example.ebook

data class NewPage(
    val id: String = "",
    val text: String = "",
    val choices: Map<String, String> = emptyMap(),
    val isFirstPage: Boolean = false
)