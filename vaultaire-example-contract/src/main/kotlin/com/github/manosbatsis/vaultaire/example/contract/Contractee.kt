package com.github.manosbatsis.vaultaire.example.contract

import com.github.manosbatsis.vaultaire.annotation.VaultaireModelDto
import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable


@CordaSerializable
@VaultaireModelDto
data class Contractee(
    var account: AnonymousParty,
    val name: String?,
    val representativeName: String?
) {}