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
package com.github.manosbatsis.vaultaire.service

import com.github.manosbatsis.vaultaire.util.DummyContract
import net.corda.core.crypto.SecureHash
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.ServiceLifecycleEvent
import net.corda.core.node.services.ServiceLifecycleEvent.STATE_MACHINE_STARTED
import net.corda.core.serialization.SingletonSerializeAsToken
import java.io.File
import java.security.MessageDigest

@CordaService
class VaultaireAttachmentService(
        private val serviceHub: AppServiceHub
) : SingletonSerializeAsToken() {
    companion object {
        private var cachedVaultaireJarHash: SecureHash? = null

        private fun getVaultaireJarHash(serviceHub: AppServiceHub): AttachmentId {
            if (cachedVaultaireJarHash == null) {
                val jarFile = File(DummyContract::class.java.protectionDomain.codeSource.location.path)
                cachedVaultaireJarHash = SecureHash.parse(hashFile(jarFile))
                if (!serviceHub.attachments.hasAttachment(cachedVaultaireJarHash!!))
                    serviceHub.attachments.importAttachment(
                            jarFile.inputStream(),
                            VaultaireAttachmentService::class.java.name,
                            jarFile.name)
            }
            return cachedVaultaireJarHash!!
        }

        private fun hashFile(input: File, algorithm: String = "SHA-256"): String {
            return MessageDigest
                    .getInstance(algorithm)
                    .digest(input.readBytes())
                    .fold("", { str, it -> str + "%02x".format(it) })
        }
    }

    init {
        serviceHub.register { processEvent(it) }
    }

    val vaultaireSecureHash: SecureHash
        get() = getVaultaireJarHash(serviceHub)

    private fun processEvent(event: ServiceLifecycleEvent) {
        // Lifecycle event handling code including full use of serviceHub
        when (event) {
            STATE_MACHINE_STARTED -> getVaultaireJarHash(serviceHub)
            else -> {
                // Process other types of events
            }
        }
    }
}
