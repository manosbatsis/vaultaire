package com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto

import com.github.manosbatsis.vaultaire.processor.dto.VaultaireDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

/** Vaultaire-specific overrides for building a "lite" DTO type spec */
open class AccountsAwareLiteDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = AccountsAwareLiteDtoStrategyComposition
)
