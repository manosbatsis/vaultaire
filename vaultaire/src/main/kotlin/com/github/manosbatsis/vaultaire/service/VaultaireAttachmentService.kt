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
