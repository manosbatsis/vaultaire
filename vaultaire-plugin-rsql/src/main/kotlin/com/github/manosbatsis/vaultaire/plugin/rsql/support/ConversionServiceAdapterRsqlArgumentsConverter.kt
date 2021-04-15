package com.github.manosbatsis.vaultaire.plugin.rsql.support

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverterFactory
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlCriterion
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.schemas.StatePersistable
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import kotlin.reflect.jvm.javaField



/**
 * An [RsqlArgumentsConverter] implementation suitable for
 * Spring and Spring Boot applications. Used as adapter for
 * an [ConversionService] instance.
 */
class ConversionServiceAdapterRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    val rootCondition: VaultQueryCriteriaCondition<P, F>,
    val conversionService: ConversionService
): RsqlArgumentsConverter<P, F> {

    companion object {
        private val logger = LoggerFactory.getLogger(RsqlArgumentsConverter::class.java)
        val stringTypeDescriptor = TypeDescriptor.valueOf(String::class.java)
    }

    class Factory<P : StatePersistable, F : Fields<P>>(
        val conversionService: ConversionService
    ) :RsqlArgumentsConverterFactory<P, F> {
        override fun create(rootCondition: VaultQueryCriteriaCondition<P, F>) =
            ConversionServiceAdapterRsqlArgumentsConverter(
                rootCondition, conversionService)
    }

    override fun convertArguments(criterion: RsqlCriterion): List<Any?> {
        val targetTypeDescriptor: TypeDescriptor = rootCondition.fields
            .fieldsByName[criterion.property]
            ?.let { wrapper ->
                wrapper.property.javaField?.let { TypeDescriptor(it) }
            }
            ?: error("Cannot convert arguments for non-existing property or field ${criterion.property}")
        return criterion.arguments.map { arg ->
            conversionService.convert(arg, stringTypeDescriptor, targetTypeDescriptor)
        }
    }

}