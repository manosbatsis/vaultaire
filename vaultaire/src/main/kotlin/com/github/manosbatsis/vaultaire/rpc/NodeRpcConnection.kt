package com.github.manosbatsis.vaultaire.rpc

import net.corda.core.messaging.CordaRPCOps

/**
 * Wraps a Corda Node RPC connection proxy
 */
interface NodeRpcConnection {
    /** Obtain a [CordaRPCOps] proxy for this connection */
    val proxy: CordaRPCOps
    /** Whether this connection should be kept private e.g. from actuator */
    fun skipInfo(): Boolean
}