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
@file:Suppress("MemberVisibilityCanBePrivate")
package com.github.manosbatsis.vaultaire.plugin.rsql.support

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverterFactory
import com.github.manosbatsis.vaultaire.plugin.rsql.anyToBoolean
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.schemas.StatePersistable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import java.util.UUID

/**
 * Simple, extensible [RsqlArgumentsConverter] implementation.
 */
open class SimpleRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    rootCondition: VaultQueryCriteriaCondition<P, F>,
    val datePattern: String = "dd-MM-yyyy",
    val datetimePattern: String = "dd-MM-yyyy hh:mm"
): AbstractRsqlArgumentsConverter<P, F>(rootCondition) {
    companion object{
        private val logger = LoggerFactory.getLogger(SimpleRsqlArgumentsConverter::class.java)
    }

    class Factory<P : StatePersistable, F : Fields<P>>() : RsqlArgumentsConverterFactory<P, F> {
        override fun create(rootCondition: VaultQueryCriteriaCondition<P, F>) =
            SimpleRsqlArgumentsConverter(rootCondition)
    }

    open val dateFormat: DateFormat = SimpleDateFormat(datetimePattern)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }

    open val dateFormatter = DateTimeFormatter.ofPattern(datePattern)

    open val dateTimeFormatter = DateTimeFormatter.ofPattern(datetimePattern)

    override fun convertItem(fieldType: Class<*>, arg: String): Any = when (fieldType) {
        String::class.java -> arg
        Int::class.java -> arg.toInt()
        Long::class.java -> arg.toLong()
        Float::class.java -> arg.toFloat()
        BigDecimal::class.java -> BigDecimal(arg)
        Boolean::class.java -> arg.anyToBoolean()
        Date::class.java -> parseDate(arg)
        LocalDate::class.java -> parseLocalDate(arg)
        LocalDateTime::class.java -> parseLocalDateTime(arg)
        UUID::class.java -> UUID.fromString(arg)
        UniqueIdentifier::class.java -> UniqueIdentifier(id = UUID.fromString(arg))
        SecureHash::class.java -> SecureHash.parse(arg)
        Class::class.java -> Class.forName(arg)
        else -> error("Cannot convert argument for unsupported type ${fieldType.canonicalName}")
    }

    open fun parseDate(date: String): Date = dateFormat.parse(date)

    open fun parseLocalDate(date: String): LocalDate = LocalDate.parse(date, dateFormatter)

    open fun parseLocalDateTime(date: String): LocalDateTime = LocalDateTime.parse(date, dateTimeFormatter)
}