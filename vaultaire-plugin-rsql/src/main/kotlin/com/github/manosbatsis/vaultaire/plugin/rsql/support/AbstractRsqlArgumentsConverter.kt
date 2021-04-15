package com.github.manosbatsis.vaultaire.plugin.rsql.support

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlCriterion
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.schemas.StatePersistable
import java.lang.reflect.Field
import kotlin.reflect.jvm.javaField

/**
 * Base [RsqlArgumentsConverter] implementation,
 * (optionally) extend to create your custom converter.
 * See [SimpleRsqlArgumentsConverter] and
 * [ObjectMapperAdapterRsqlArgumentsConverter] for examples.
 */
abstract class AbstractRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    val rootCondition: VaultQueryCriteriaCondition<P, F>
): RsqlArgumentsConverter<P, F> {

    abstract fun convertItem(fieldType: Class<*>, arg: String): Any

    override fun convertArguments(criterion: RsqlCriterion): List<*> {
        val field: Field = rootCondition.fields.fieldsByName[criterion.property]?.property?.javaField
            ?: error("Cannot convert arguments for non-existing property or field ${criterion.property}")
        val fieldType: Class<*> = field.type
        return criterion.arguments.map { arg ->
            if(arg == null) arg
            else convertItem(fieldType, arg)
        }
    }
}