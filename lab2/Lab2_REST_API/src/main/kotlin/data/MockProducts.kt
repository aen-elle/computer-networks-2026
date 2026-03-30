package org.example.data

import java.util.UUID

val MOCK_PRODUCTS: MutableList<Product> = mutableListOf(
    Product(
        id = UUID.randomUUID().toString(),
        name = "Item 1",
        description = "Mock Item 1"
    ),
    Product(
        id = UUID.randomUUID().toString(),
        name = "Item 2",
        description = "Mock Item 2"
    ),
    Product(
        id = UUID.randomUUID().toString(),
        name = "Item 3",
        description = "Mock Item 3"
    )
)