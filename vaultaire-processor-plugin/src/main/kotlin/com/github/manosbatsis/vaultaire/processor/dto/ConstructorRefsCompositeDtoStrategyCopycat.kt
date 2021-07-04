package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.*
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.*
import kotlin.reflect.KFunction1


/** Used for flexible cloning of [ConstructorRefsCompositeDtoStrategy] at runtime, e.g. for views */
abstract class ConstructorRefsCompositeDtoStrategyCopycat(
        originalStrategy: ConstructorRefsCompositeDtoStrategy<*,*,*>,
        annotatedElementInfo: AnnotatedElementInfo = originalStrategy.annotatedElementInfo,
        dtoNameStrategyConstructor: KFunction1<AnnotatedElementInfo, DtoNameStrategy> = originalStrategy.dtoNameStrategyConstructor,
        dtoTypeStrategyConstructor: KFunction1<AnnotatedElementInfo, DtoTypeStrategy> = originalStrategy.dtoTypeStrategyConstructor,
        dtoMembersStrategyConstructor: KFunction1<DtoStrategyLesserComposition, DtoMembersStrategy> = originalStrategy.dtoMembersStrategyConstructor
): ConstructorRefsCompositeDtoStrategy<DtoNameStrategy, DtoTypeStrategy, DtoMembersStrategy>(
        annotatedElementInfo, dtoNameStrategyConstructor, dtoTypeStrategyConstructor, dtoMembersStrategyConstructor
){

    override fun dtoTypeSpecBuilder(): TypeSpec.Builder {
        val dtoTypeSpecBuilder = TypeSpec.classBuilder(getClassName())
        addSuperTypes(dtoTypeSpecBuilder)
        addModifiers(dtoTypeSpecBuilder)
        addKdoc(dtoTypeSpecBuilder)
        addAnnotations(dtoTypeSpecBuilder)
        addMembers(dtoTypeSpecBuilder)
        annotatedElementInfo.primaryTargetTypeElement.typeParameters.forEach {
            dtoTypeSpecBuilder.addTypeVariable(
                    TypeVariableName.invoke(it.simpleName.toString(), *it.bounds.map { it.asTypeName() }.toTypedArray()))
        }

        return dtoTypeSpecBuilder
    }

}