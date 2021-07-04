package com.github.manosbatsis.vaultaire.annotation

import net.corda.core.flows.FlowLogic
import kotlin.reflect.KClass

/**
 * Generate a responser flow that extends the given type.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireFlowResponder(
        val value: KClass<out FlowLogic<*>>,
        val comment: String = ""
)