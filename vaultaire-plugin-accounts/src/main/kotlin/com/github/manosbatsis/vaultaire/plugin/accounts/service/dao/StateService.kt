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
package com.github.manosbatsis.vaultaire.plugin.accounts.service.dao

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeService
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.dao.ExtendedStateService
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.contracts.ContractState
import net.corda.core.schemas.StatePersistable
import net.corda.core.utilities.contextLogger


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface AccountsAwareStateService<T : ContractState> : AccountsAwareNodeService,
        StateService<T>,
        AccountsAwareStateServiceDelegate<T> {

    companion object {
        private val logger = contextLogger()
    }

}


/**
 * Basic [StateService] implementation, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
open class BasicAccountsAwareStateService<T : ContractState>(
        delegate: AccountsAwareStateServiceDelegate<T>
) : BasicStateService<T>(delegate),
        AccountsAwareStateServiceDelegate<T> by delegate,
        AccountsAwareStateService<T> {


}

/**
 * Extends [BasicStateService] to provide a [StateService] aware of the target
 * [ContractState] type's [StatePersistable] and [Fields].
 *
 * Subclassed by Vaultaire's annotation processing to generate service components.
 */
abstract class ExtendedAccountsAwareStateService<T : ContractState, P : StatePersistable, out F : Fields<P>, Q : VaultQueryCriteriaCondition<P, F>>(
        delegate: AccountsAwareStateServiceDelegate<T>
) : BasicAccountsAwareStateService<T>(delegate), ExtendedStateService<T, P, F, Q> {

    /** The fields of the target [StatePersistable] type `P` */
    override lateinit var criteriaConditionsType: Class<Q>
}
