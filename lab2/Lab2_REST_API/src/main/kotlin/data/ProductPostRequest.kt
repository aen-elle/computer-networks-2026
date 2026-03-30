package org.example.data

import kotlinx.serialization.Serializable

@Serializable
data class ProductPostRequest (
    val name: String,
    val description: String,
)

@Serializable
data class ProductPutRequest (
    val id: String? = null,
    val name: String? = null,
    val description: String? = null
)