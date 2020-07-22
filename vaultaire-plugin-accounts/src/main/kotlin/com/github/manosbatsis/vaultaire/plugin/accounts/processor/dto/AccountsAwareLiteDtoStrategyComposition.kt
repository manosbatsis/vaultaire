package com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto

import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategyComposition
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

open class AccountsAwareLiteDtoStrategyComposition(
        override val annotatedElementInfo: AnnotatedElementInfo
): DtoStrategyComposition {

    override val dtoNameStrategy = LiteDtoNameStrategy(
            annotatedElementInfo
    )
    override val dtoTypeStrategy = AccountAwareLiteDtoTypeStrategy(
            annotatedElementInfo
    )
    override val dtoMembersStrategy = AcountsAwareLiteDtoMemberStrategy(
            annotatedElementInfo,
            dtoNameStrategy,
            dtoTypeStrategy
    )
}
