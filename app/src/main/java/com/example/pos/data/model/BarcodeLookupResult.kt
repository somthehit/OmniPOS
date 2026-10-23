package com.example.pos.data.model

import com.example.pos.data.local.ProductEntity

data class BarcodeLookupResult(
    val found: Boolean,
    val item: ProductEntity? = null,
    val packQty: Int = 1,
    val barcode: String = "",
    val isSecondaryBarcode: Boolean = false
)
