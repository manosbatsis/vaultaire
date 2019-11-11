package com.github.manosbatsis.vaultaire.registry

import com.github.manosbatsis.vaultaire.dao.ExtendedStateService
import com.github.manosbatsis.vaultaire.dsl.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.contracts.ContractState
import net.corda.core.schemas.StatePersistable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object Registry {

    private val services: MutableMap<KClass<*>, KClass<*>> = ConcurrentHashMap()
    private val queryDsls: MutableMap<KClass<*>, KClass<*>> = ConcurrentHashMap()

    fun <T: ContractState, P : StatePersistable, F: Fields<P>, Q: VaultQueryCriteriaCondition<P, F>, S: ExtendedStateService<T, P, F, Q>> registerService(
            keyType: KClass<T>, serviceType: KClass<S>
    ) = services.put(keyType, serviceType)

    fun <P : StatePersistable, F: Fields<P>, Q: VaultQueryCriteriaCondition<P, F>> registerQueryDsl(
            keyType: KClass<P>, queryCriteriaType: KClass<Q>
    ) = queryDsls.put(keyType, queryCriteriaType)

}