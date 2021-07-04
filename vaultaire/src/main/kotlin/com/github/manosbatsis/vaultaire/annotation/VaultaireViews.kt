package com.github.manosbatsis.vaultaire.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireViews(
        val value: Array<VaultaireView>,
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO, VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO],
        val nonDataClass: Boolean = false

)

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class VaultaireView(
        val name: String,
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