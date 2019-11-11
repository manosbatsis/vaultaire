package com.github.manosbatsis.vaultaire.registry

import com.github.manosbatsis.vaultaire.dao.StateService
import net.corda.core.contracts.ContractState
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object Registry {

    private val services: MutableMap<KClass<*>, KClass<*>> = ConcurrentHashMap()
    private val queryDsls: MutableMap<KClass<*>, KClass<*>> = ConcurrentHashMap()

    fun <T : ContractState, S: StateService<T>> registerService(keyType: KClass<T>, serviceType: KClass<S>){
        val result = services.put(keyType, serviceType)
    }

}