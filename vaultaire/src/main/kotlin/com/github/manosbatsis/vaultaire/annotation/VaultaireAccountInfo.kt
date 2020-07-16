package com.github.manosbatsis.vaultaire.annotation

/**
 * Marks a [net.corda.core.contracts.ContractState] property as a Corda Account.
 * Supported property types are [java.security.PublicKey], [net.corda.core.identity.AbstractParty]
 * and [net.corda.core.identity.AnonymousParty].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class VaultaireAccountInfo(
)

