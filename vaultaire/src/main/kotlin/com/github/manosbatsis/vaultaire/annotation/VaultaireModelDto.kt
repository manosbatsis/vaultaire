package com.github.manosbatsis.vaultaire.annotation

import kotlin.reflect.KClass

/** Generate a REST-friendly DTO for the annotated input model class or constructor. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireModelDto(
        val ignoreProperties: Array<String> = [],
        val copyAnnotationPackages: Array<String> = [],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)

/** Generate a REST-friendly DTO for the target [baseType] input model class of a project dependency. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireModelDtoMixin(
        val ignoreProperties: Array<String> = [],
        val baseType: KClass<out Any>,
        val copyAnnotationPackages: Array<String> = [],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)