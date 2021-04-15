package com.github.manosbatsis.vaultaire.plugin.rsql

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.util.Fields
import cz.jirutka.rsql.parser.RSQLParser
import net.corda.core.schemas.StatePersistable

/**
 * [VaultQueryCriteriaCondition] extension that
 * adds the given RSQL filter to the current instance,
 * then returns it for chaining.
 *
 * Example:
 * ```kotlin
 * bookQuery{
 *    status = Vault.StateStatus.ALL
 *    // ...
 *    orderBy {
 *        fields.title sort DESC
 *    }
 * }
 * // Add the RSQL query filter
 * .withRsql("authorFirst==john;authorLast==doe")
 * // Convert all the above
 * // to Corda QueryCriteria
 * .toCriteria()
 * ```
 */
fun <P : StatePersistable, F : Fields<P>, C: VaultQueryCriteriaCondition<P, F>> C.withRsql(
    rsql: String?, converterFactory: RsqlArgumentsConverterFactory<P, F>
): C {
    if(!rsql.isNullOrBlank()) {
        RSQLParser(RsqlSearchOperation.asSimpleOperators)
            .parse(rsql)
            .accept(RsqlConditionsVisitor(
                this,
                converterFactory.create(this)))
            ?.also { this.addCondition(it) }
    }
    return this
}
