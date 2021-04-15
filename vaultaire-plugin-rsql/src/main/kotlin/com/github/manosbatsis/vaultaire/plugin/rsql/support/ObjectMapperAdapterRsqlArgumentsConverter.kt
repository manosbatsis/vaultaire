package com.github.manosbatsis.vaultaire.plugin.rsql.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverterFactory
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.schemas.StatePersistable


/**
 * An [RsqlArgumentsConverter] implementation suitable for
 * applications thar make use of Jackson. Used as adapter for
 * an [ObjectMapper] instance.
 */
class ObjectMapperAdapterRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    rootCondition: VaultQueryCriteriaCondition<P, F>,
    private val objectMapper: ObjectMapper
): AbstractRsqlArgumentsConverter<P, F>(rootCondition) {

    class Factory<P : StatePersistable, F : Fields<P>>(
        val objectMapper: ObjectMapper
    ) : RsqlArgumentsConverterFactory<P, F> {
        override fun create(rootCondition: VaultQueryCriteriaCondition<P, F>) =
            ObjectMapperAdapterRsqlArgumentsConverter(
                rootCondition, objectMapper)
    }

    override fun convertItem(fieldType: Class<*>, arg: String): Any =
        objectMapper.readValue(arg, fieldType)
}