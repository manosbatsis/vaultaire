/*
 * Vaultaire: query DSL and data access utilities for Corda developers.
 * Copyright (C) 2018 Manos Batsis
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package com.github.manosbatsis.vaultaire.registry

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.service.dao.ExtendedStateService
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.contracts.ContractState
import net.corda.core.schemas.StatePersistable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object Registry {

    private val services: MutableMap<Class<*>, KClass<*>> = ConcurrentHashMap()
    private val queryDsls: MutableMap<KClass<*>, KClass<*>> = ConcurrentHashMap()

    fun <T : ContractState, P : StatePersistable, F : Fields<P>, Q : VaultQueryCriteriaCondition<P, F>, S : ExtendedStateService<T, P, F, Q>> registerService(
            keyType: KClass<T>, serviceType: KClass<S>
    ) = services.put(keyType.java, serviceType)

    fun <P : StatePersistable, F : Fields<P>, Q : VaultQueryCriteriaCondition<P, F>> registerQueryDsl(
            keyType: KClass<P>, queryCriteriaType: KClass<Q>
    ) = queryDsls.put(keyType, queryCriteriaType)

    fun <T : ContractState, S : StateService<T>>
            getStateServiceType(
            contractStateType: Class<T>
    ): Class<S>? {
        return services[contractStateType] as Class<S>?
    }

}
