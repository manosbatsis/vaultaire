package com.github.manosbatsis.vaultaire.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireViews(
        val value: Array<VaultaireView>,
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO, VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO],
        val nonDataClass: Boolean = false

)


/**
 * Definition of a view as annotation, The generated classname is created as follows:
 *
 * - By default both [name] and [nameSuffix] are empty.
 * If neither is given an error will be thrown during annotation processing.
 * - If only [nameSuffix] is given, it will replace the target type's (DTO) suffix.
 * - When a [name] is given, the target classname is `"$name$nameSuffix"`
 *
 * The fields are filtered using [includeNamedFields] and [excludeNamedFields].
 * Both can be used concurrently to define the subset of fields for the generated view.
 *
 * The [viewFields] provide some more control and are included by default,
 * the latter is controlled per field by  [VaultaireViewField.ignoreIfNotIncludeNamedField].
 *
 */
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class VaultaireView(
        val name: String = "",
        val nameSuffix: String = "",
        val includeNamedFields: Array<String> = [],
        val excludeNamedFields: Array<String> = [],
        val viewFields: Array<VaultaireViewField> = [],
        val excludeStrategies: Array<VaultaireDtoStrategyKeys> = []
)

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class VaultaireViewField(
        val name: String,
        val nonNull: Boolean = false,
        val ignoreIfNotIncludeNamedField: Boolean = false
)