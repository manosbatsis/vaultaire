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
// -------------------- DO NOT EDIT -------------------
//  This file is automatically generated by Vaultaire,
//  see https://manosbatsis.github.io/vaultaire
// ----------------------------------------------------
package com.github.manosbatsis.vaultaire.plugin.accounts.dto

import com.github.manosbatsis.kotlin.utils.api.Dto
import com.github.manosbatsis.kotlin.utils.api.DtoInsufficientMappingException
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

/**
 * A [AccountInfo]-specific [Dto] implementation
 */
@CordaSerializable
data class AccountInfoDto(
        var name: String? = null,
        var host: Party? = null,
        var identifier: UniqueIdentifier? = null
) : Dto<AccountInfo> {
    /**
     * Create a patched copy of the given [AccountInfo] instance,
     * updated using this DTO's non-null properties.
     */
    override fun toPatched(original: AccountInfo): AccountInfo {
        val patched = AccountInfo(
                name = this.name ?: original.name,
                host = this.host ?: original.host,
                identifier = this.identifier ?: original.identifier
        )
        return patched
    }

    /**
     * Create an instance of [AccountInfo], using this DTO's properties.
     * May throw a [DtoInsufficientStateMappingException]
     * if there is mot enough information to do so.
     */
    override fun toTargetType(): AccountInfo {
        try {
            return AccountInfo(
                    name = this.name!!,
                    host = this.host!!,
                    identifier = this.identifier!!
            )
        } catch (e: Exception) {
            throw DtoInsufficientMappingException(exception = e)
        }
    }

    companion object {
        /**
         * Create a new DTO instance using the given [AccountInfo] as source.
         */
        fun mapToDto(original: AccountInfo): AccountInfoDto =
                com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoDto(
                        name = original.name,
                        host = original.host,
                        identifier = original.identifier
                )

    }
}
