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
package com.github.manosbatsis.vaultaire.example


import com.github.manosbatsis.vaultaire.dao.*
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub


class CustomBasicBookStateService(
        delegate: StateServiceDelegate<BookContract.BookState>
) : BasicStateService<BookContract.BookState>(delegate){

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, BookContract.BookState::class.java, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, BookContract.BookState::class.java, defaults))

    // Custom business methods...
}
