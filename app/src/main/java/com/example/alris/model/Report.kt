package com.example.alris.model

data class Report(
    val id:String,
    val filename: String?=null,
    val description: String?=null,
    val latitude: Double?,
    val longitude: Double?,
    val label: String,
    val is_spam: Boolean,
    val is_fake: Boolean,
    val public_url: String,
    val created_at: String
)

