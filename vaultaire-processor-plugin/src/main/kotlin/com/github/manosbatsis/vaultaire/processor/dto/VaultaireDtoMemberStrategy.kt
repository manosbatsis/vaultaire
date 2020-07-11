package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoMembersStrategy
import javax.annotation.processing.ProcessingEnvironment

open class VaultaireDtoMemberStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
): SimpleDtoMembersStrategy(processingEnvironment, dtoInputContext){

}
