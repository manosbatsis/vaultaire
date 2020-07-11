package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoTypeStrategy
import javax.annotation.processing.ProcessingEnvironment

open class VaultaireDtoTypeStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
): SimpleDtoTypeStrategy(processingEnvironment, dtoInputContext){

}
