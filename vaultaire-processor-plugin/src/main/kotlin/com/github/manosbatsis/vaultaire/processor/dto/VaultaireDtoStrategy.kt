package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.*
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        composition: DtoStrategyComposition = VaultaireDefaultDtoStrategyComposition()
) : CompositeDtoStrategy(annotatedElementInfo,composition)

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDefaultDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireDefaultDtoStrategyComposition()
)
/** Vaultaire-specific overrides for building a "lite" DTO type spec */
class VaultaireLiteDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : CompositeDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireLiteDtoStrategyComposition
)

open class VaultaireDefaultDtoStrategyComposition: SimpleDtoStrategyComposition() {
    override fun dtoMembersStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoMembersStrategy = VaultaireDtoMemberStrategy(
            annotatedElementInfo
    )
}

object VaultaireLiteDtoStrategyComposition: DtoStrategyComposition {
    override fun dtoNameStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoNameStrategy = LiteDtoNameStrategy(annotatedElementInfo)
    override fun dtoMembersStrategy(annotatedElementInfo: AnnotatedElementInfo): DtoMembersStrategy = VaultaireLiteDtoMemberStrategy(annotatedElementInfo)
    override fun dtoTypeStrategy(annotatedElementInfo: AnnotatedElementInfo): DtoTypeStrategy = VaultaireLiteDtoTypeStrategy(annotatedElementInfo)
}
