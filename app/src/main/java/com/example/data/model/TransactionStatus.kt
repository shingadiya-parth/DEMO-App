package com.example.data.model

/**
 * Status of a coin ledger transaction.
 */
enum class TransactionStatus(val label: String) {
    COMPLETED("Completed"),
    PENDING("Processing"),
    FAILED("Failed"),
    REVERSED("Reversed")
}
