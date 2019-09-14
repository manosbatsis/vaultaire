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
package com.github.manosbatsis.vaultaire.util

import net.corda.core.schemas.StatePersistable
import kotlin.reflect.KProperty1

/**
 * Wraps a non-nullable [KProperty1] belonging to a [StatePersistable] to provide for cleaner operators,
 * i.e. without conflicting [net.corda.core.node.services.vault.Builder]
 */
class GenericFieldWrapper<T : StatePersistable, S>(override val property: KProperty1<T, S>): TypedFieldWrapper<T, S>

/**
 * Wraps a nullable [KProperty1] belonging to a [StatePersistable] to provide for cleaner operators,
 * i.e. without conflicting [net.corda.core.node.services.vault.Builder]
 */
class NullableGenericFieldWrapper<T : StatePersistable, S>(override val property: KProperty1<T, S?>): TypedFieldWrapper<T, S?>

/**
 * Wraps a [KProperty1] belonging to a [StatePersistable] to provide for cleaner operators,
 * i.e. without conflicting [net.corda.core.node.services.vault.Builder]
 */
interface TypedFieldWrapper<T : StatePersistable, S>: FieldWrapper<T>{
    override val property: KProperty1<T, S>
}

interface FieldWrapper<T : StatePersistable>{
    val property: KProperty1<T, *>
}

/** Extended by Vaultaire's annotation processing to provide easy access to fields of a [StatePersistable] type */
interface Fields<T : StatePersistable>{

    val fieldsByName: Map<String, FieldWrapper<T>>

    fun contains(name: String) = fieldsByName.contains(name)

    @Suppress("UNCHECKED_CAST")
    operator fun get(name: String): FieldWrapper<T> =
            fieldsByName[name] ?: throw IllegalArgumentException("Field not found: $name")
}
