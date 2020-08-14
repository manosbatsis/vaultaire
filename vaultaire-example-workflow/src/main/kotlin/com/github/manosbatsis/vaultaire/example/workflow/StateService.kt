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
package com.github.manosbatsis.vaultaire.example.workflow


import com.github.manosbatsis.vaultaire.example.contract.BookContract
import com.github.manosbatsis.vaultaire.example.contract.BookStateCordaServiceDelegate
import com.github.manosbatsis.vaultaire.example.contract.BookStateService
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateServiceRpcDelegate
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.dao.StateServiceDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceHubDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceRpcDelegate
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub


class CustomBasicBookStateService(
        delegate: StateServiceDelegate<BookContract.BookState>
) : BasicStateService<BookContract.BookState>(delegate) {

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: ServiceDefaults = SimpleServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, BookContract.BookState::class.java, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, defaults: ServiceDefaults = SimpleServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, BookContract.BookState::class.java, defaults))

    // Custom business methods...
}

/** Extend the generated [BookStateService] */
class MyExtendedBookStateService(
        delegate: StateServiceDelegate<BookContract.BookState>
) : BookStateService(delegate) {

    // Add the appropriate constructors
    // to initialize per delegate type:

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: ServiceDefaults = SimpleServiceDefaults()
    ) : this(AccountsAwareStateServiceRpcDelegate(rpcOps, BookContract.BookState::class.java, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, defaults: ServiceDefaults = SimpleServiceDefaults()
    ) : this(serviceHub.cordaService(BookStateCordaServiceDelegate::class.java))

    // Custom business methods...
}
