package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.CompositeDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategyComposition
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoStrategyComposition
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import javax.lang.model.element.VariableElement

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        composition: DtoStrategyComposition =
                VaultaireDefaultDtoStrategyComposition(annotatedElementInfo)
) : CompositeDtoStrategy(
        annotatedElementInfo,composition
), ProcessingEnvironmentAware, AnnotatedElementInfo by annotatedElementInfo {


    override fun getFieldsToProcess(): List<VariableElement> {
        val includeParticipants = annotatedElementInfo.annotation
                .getAnnotationValue("includeParticipants").value as Boolean
        processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, ignoreParticipants: $includeParticipants" }
        processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, ignoreProperties: $ignoreProperties" }
        val ignored = if (includeParticipants) ignoreProperties else ignoreProperties + "participants"
        processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, ignored: $ignored" }
        return primaryTargetTypeElementFields.filterNot { ignored.contains(it.simpleName.toString()) }
                .map{
                    processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, includiong: ${it.simpleName}" }
                    it
                }
    }
}

/** Vaultaire-specific overrides for building a DTO type spec */
open class DefaultDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireDefaultDtoStrategyComposition(annotatedElementInfo)
)
/** Vaultaire-specific overrides for building a "lite" DTO type spec */
class LiteDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireLiteDtoStrategyComposition(annotatedElementInfo)
)

open class VaultaireDefaultDtoStrategyComposition(
        annotatedElementInfo: AnnotatedElementInfo
): SimpleDtoStrategyComposition(annotatedElementInfo) {

    override val dtoMembersStrategy = VaultaireDtoMemberStrategy(
            annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy
    )
}

open class VaultaireLiteDtoStrategyComposition(
        override val annotatedElementInfo: AnnotatedElementInfo
): DtoStrategyComposition {

    override val dtoNameStrategy = LiteDtoNameStrategy(
            annotatedElementInfo
    )
    override val dtoTypeStrategy = LiteDtoTypeStrategy(annotatedElementInfo)
    override val dtoMembersStrategy = LiteDtoMemberStrategy(
            annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy
    )
}
