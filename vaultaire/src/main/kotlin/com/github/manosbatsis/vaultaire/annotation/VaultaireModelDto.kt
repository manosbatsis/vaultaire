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
package com.github.manosbatsis.vaultaire.annotation

import kotlin.reflect.KClass

/** Generate a REST-friendly DTO for the annotated input model class or constructor. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireModelDto(
        val ignoreProperties: Array<String> = [],
        val copyAnnotationPackages: Array<String> = [],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)

/** Generate a REST-friendly DTO for the target [baseType] input model class of a project dependency. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireModelDtoMixin(
        val ignoreProperties: Array<String> = [],
        val baseType: KClass<out Any>,
        val copyAnnotationPackages: Array<String> = [],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)