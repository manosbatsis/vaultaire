package com.github.manosbatsis.vaultaire.util

/** Occurs when a DTO has insufficient information for mapping to a state */
class DtoInsufficientStateMappingException(
        message: String = "Insufficient information while mapping DTO to state",
        exception: Exception? = null) : RuntimeException(message, exception)