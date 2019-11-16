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
package com.github.manosbatsis.vaultaire.dsl.contract

import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.contracts.ContractState
import net.corda.core.transactions.LedgerTransaction

class When(vararg conditions: Boolean){
}

abstract class ContractConditions(
        val tx: LedgerTransaction
){
    abstract val commandTypes: Any
    abstract val stateTypes: Any
    abstract val stateTypeFields: Any
    abstract val groups: Any
    abstract val roles: Any

    abstract val <T: ContractState> LedgerTransaction.InOutGroup<T, *>.fields: Fields<T>


}

