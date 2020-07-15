package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

open class LiteDtoNameStrategy(
        annotatedElementInfo: AnnotatedElementInfo
): SimpleDtoNameStrategy(annotatedElementInfo) {

    override fun getClassNameSuffix(): String = "LiteDto"
}
