package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.*
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import javax.lang.model.element.VariableElement

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        composition: DtoStrategyComposition = VaultaireDefaultDtoStrategyComposition()
) : CompositeDtoStrategy(annotatedElementInfo,composition){

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
        composition = VaultaireDefaultDtoStrategyComposition()
)
/** Vaultaire-specific overrides for building a "lite" DTO type spec */
class LiteDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
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
    override fun dtoMembersStrategy(annotatedElementInfo: AnnotatedElementInfo): DtoMembersStrategy = LiteDtoMemberStrategy(annotatedElementInfo)
    override fun dtoTypeStrategy(annotatedElementInfo: AnnotatedElementInfo): DtoTypeStrategy = LiteDtoTypeStrategy(annotatedElementInfo)
}
