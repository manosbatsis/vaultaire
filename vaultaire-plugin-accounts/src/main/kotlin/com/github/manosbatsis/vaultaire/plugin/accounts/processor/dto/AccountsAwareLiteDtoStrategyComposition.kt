package com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto

import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategyComposition
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

object AccountsAwareLiteDtoStrategyComposition: DtoStrategyComposition {
    override fun dtoNameStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoNameStrategy = LiteDtoNameStrategy(
            annotatedElementInfo
    )
    override fun dtoMembersStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoMembersStrategy = AcountsAwareLiteDtoMemberStrategy(
            annotatedElementInfo
    )
    override fun dtoTypeStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoTypeStrategy = AccountAwareLiteDtoTypeStrategy(
            annotatedElementInfo
    )
}
