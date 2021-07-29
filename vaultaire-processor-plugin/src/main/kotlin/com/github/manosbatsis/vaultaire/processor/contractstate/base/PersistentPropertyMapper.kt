package com.github.manosbatsis.vaultaire.processor.contractstate.base

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manosbatsis.vaultaire.annotation.PersistenrPropertyMappingMode
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StaticPointer
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import javax.lang.model.element.VariableElement


interface PersistentPropertyMapper<T> {

    fun supportedTypes(): List<String>
    fun map(original: MappedProperty): List<MappedProperty>
    fun map(
            original: MappedProperty,
            modes: List<PersistenrPropertyMappingMode> = listOf(PersistenrPropertyMappingMode.NATIVE)
    ): List<MappedProperty>
}


data class MappedProperty(
        val propertyName: String,
        val propertyPath: List<String> = listOf(propertyName),
        val propertyType: TypeName,
        val propertyDefaults: Pair<String, Boolean>? = null,
        val fieldIndex: Int,
        val variableElement: VariableElement,
        val asString: Boolean = false
)

abstract class BasePersistentPropertyMapper<T>(
        val delegate: BaseStateMembersStrategy
): PersistentPropertyMapper<T>, ProcessingEnvironmentAware by delegate{

    override fun map(original: MappedProperty): List<MappedProperty>{
        return map(original, delegate.getPersistentMappingModes(original.variableElement))
    }

    fun MappedProperty.asChildMappedProperty(
            subPath: List<String>,
            childPropertyName: String = "${propertyName}${subPath.map { it.capitalize() }.joinToString("")}",
            asString: Boolean = false
    ): MappedProperty{

        println("asChildMappedProperty, propertyName: $propertyName, childPropertyName: $childPropertyName")
        var contextFieldElement = variableElement
        subPath.forEach { pathStep ->
            println("asChildMappedProperty, pathStep: $pathStep")
            val contextFieldTypeElement = contextFieldElement.asType().asTypeElement()
            println("asChildMappedProperty, pathStep: $pathStep, contextFieldTypeElement: $contextFieldTypeElement")
            val contextFieldChildren = contextFieldTypeElement.accessibleConstructorParameterFields(true)
            println("asChildMappedProperty, pathStep: $pathStep, contextFieldChildren(${contextFieldChildren.size}): ${contextFieldChildren.joinToString(",") { "${it.simpleName}" }}")
            contextFieldElement = contextFieldChildren.map {
                        val match = "${it.simpleName}" == pathStep
                        println("asChildMappedProperty, pathStep: $pathStep, field: ${it.simpleName}, match: $match")
                        it
                    }
                    .find { "${it.simpleName}" == pathStep  }
                    ?: error("Could not find field $pathStep in ${contextFieldTypeElement.asKotlinClassName().canonicalName}")
        }
        val childPropertyDefaults = delegate.toDefaultValueExpression(contextFieldElement)
        println("asChildMappedProperty, childPropertyDefaults: $childPropertyDefaults")
        val childPropertyType = delegate.rootDtoMembersStrategy
                .toPropertyTypeName(contextFieldElement)
                .let { propType ->
                    if (childPropertyDefaults != null) propType.copy(nullable = childPropertyDefaults.second)
                    else propType
                }
        println("asChildMappedProperty, childPropertyType: $childPropertyType")
        return MappedProperty(
                propertyName = childPropertyName,
                propertyPath = propertyPath + subPath,
                propertyType = childPropertyType,
                fieldIndex = fieldIndex,
                propertyDefaults = childPropertyDefaults,
                variableElement = contextFieldElement,
                asString = asString
        )
    }
}

class AccountPartyPersistentPropertyMapper(
        delegate: BaseStateMembersStrategy
): BasePersistentPropertyMapper<AccountParty>(delegate){
    override fun  supportedTypes() = listOf(AccountParty::class.java.canonicalName)

    override fun map(original: MappedProperty, modes: List<PersistenrPropertyMappingMode>): List<MappedProperty> {
        return listOfNotNull(
                original.asChildMappedProperty(
                        subPath = listOf("identifier")
                ),
                original.asChildMappedProperty(
                        subPath = listOf("name")
                ),
                if(modes.contains(PersistenrPropertyMappingMode.EXPANDED))
                    original.asChildMappedProperty(subPath = listOf("externalId"))
                else null
        )
    }
}

open class CordaX500NamePersistentPropertyMapper(
    delegate: BaseStateMembersStrategy
): BasePersistentPropertyMapper<CordaX500Name>(delegate){

    override fun  supportedTypes() = listOf(CordaX500Name::class.java.canonicalName)

    override fun map(original: MappedProperty, modes: List<PersistenrPropertyMappingMode>): List<MappedProperty> {
        return listOfNotNull(
                if(modes.contains(PersistenrPropertyMappingMode.NATIVE))
                    original
                else null,
                if(modes.contains(PersistenrPropertyMappingMode.STRINGIFY))
                    original.copy(asString = true, propertyName = "${original.propertyName}String")
                else null
        ) + if(modes.contains(PersistenrPropertyMappingMode.EXPANDED))
            listOf(
                    original.asChildMappedProperty(subPath = listOf("commonName")),
                    original.asChildMappedProperty(subPath = listOf("organisationUnit")),
                    original.asChildMappedProperty(subPath = listOf("organisation")),
                    original.asChildMappedProperty(subPath = listOf("locality")),
                    original.asChildMappedProperty(subPath = listOf("state")),
                    original.asChildMappedProperty(subPath = listOf("country"))
            )
        else emptyList()
    }
}

class PartyPersistentPropertyMapper(
        delegate: BaseStateMembersStrategy
): BasePersistentPropertyMapper<Party>(delegate){

    override fun  supportedTypes() = listOf(Party::class.java.canonicalName)

    override fun map(partyProp: MappedProperty, modes: List<PersistenrPropertyMappingMode>): List<MappedProperty> {

        return listOfNotNull(if(modes.contains(PersistenrPropertyMappingMode.NATIVE))
            partyProp
        else null) + CordaX500NamePersistentPropertyMapper(delegate)
                .map(partyProp.asChildMappedProperty(
                        subPath = listOf("name")
                ), modes.filterNot { it == PersistenrPropertyMappingMode.NATIVE })
    }
}

class LinearPointerPersistentPropertyMapper(
        delegate: BaseStateMembersStrategy
): BasePersistentPropertyMapper<LinearPointer<*>>(delegate){

    override fun  supportedTypes() = listOf(LinearPointer::class.java.canonicalName)

    override fun map(original: MappedProperty, modes: List<PersistenrPropertyMappingMode>): List<MappedProperty> {
        return listOfNotNull(
                original.asChildMappedProperty(
                        subPath = listOf("pointer", "id"),
                        childPropertyName = original.propertyName
                )
        ) + if(modes.contains(PersistenrPropertyMappingMode.EXPANDED))
            listOf(
                    original.asChildMappedProperty(subPath = listOf("type")),
                    original.asChildMappedProperty(subPath = listOf("resolved"))
            )
        else emptyList()
    }
}


class StaticPointerPersistentPropertyMapper(
        delegate: BaseStateMembersStrategy
): BasePersistentPropertyMapper<StaticPointer<*>>(delegate){

    override fun  supportedTypes() = listOf(StaticPointer::class.java.canonicalName)
/*

        override val pointer: StateRef,
        override val type: Class<T>,
        override val isResolved: Boolean = false
 */
    override fun map(original: MappedProperty, modes: List<PersistenrPropertyMappingMode>): List<MappedProperty> {
        return listOfNotNull(
                if(modes.contains(PersistenrPropertyMappingMode.NATIVE)) original else null,
                if(modes.contains(PersistenrPropertyMappingMode.STRINGIFY))
                    original.asChildMappedProperty(
                            subPath = listOf("txhash"),
                            asString = true,
                            childPropertyName = original.propertyName)
                else null,
                if(modes.contains(PersistenrPropertyMappingMode.STRINGIFY))
                    original.asChildMappedProperty(
                            subPath = listOf("index"),
                            childPropertyName = original.propertyName)
                else null
                /*
                val txhash: SecureHash, val index: Int
                 */
        ) + if(modes.contains(PersistenrPropertyMappingMode.EXPANDED))
            listOf(
                    original.asChildMappedProperty(subPath = listOf("type")),
                    original.asChildMappedProperty(subPath = listOf("resolved"))
            )
        else emptyList()
    }
}



class IdentityPersistentPropertyMapper(
        delegate: BaseStateMembersStrategy
): BasePersistentPropertyMapper<Party>(delegate){

    override fun  supportedTypes() = listOf(
            Int::class.java.canonicalName,
            java.lang.Integer::class.java.canonicalName,
            Long::class.java.canonicalName,
            java.lang.Long::class.java.canonicalName,
            Float::class.java.canonicalName,
            java.lang.Float::class.java.canonicalName,
            Short::class.java.canonicalName,
            java.lang.Short::class.java.canonicalName,
            Double::class.java.canonicalName,
            java.lang.Double::class.java.canonicalName,
            java.math.BigDecimal::class.java.canonicalName,
            String::class.java.canonicalName,
            java.lang.String::class.java.canonicalName,
            Boolean::class.java.canonicalName,
            java.lang.Boolean::class.java.canonicalName,
            Byte::class.java.canonicalName,
            java.lang.Byte::class.java.canonicalName,
            java.util.Date::class.java.canonicalName,
            java.sql.Date::class.java.canonicalName,
            java.util.Calendar::class.java.canonicalName,
            Array<Byte>::class.java.asTypeName().toString(),
            java.sql.Clob::class.java.canonicalName,
            java.sql.Blob::class.java.canonicalName,
            java.util.TimeZone::class.java.canonicalName,
            java.util.Currency::class.java.canonicalName,
            java.lang.Class::class.java.canonicalName,
            java.time.LocalTime::class.java.canonicalName,
            java.time.LocalDate::class.java.canonicalName,
            java.time.LocalDateTime::class.java.canonicalName,
            java.time.Instant::class.java.canonicalName,
            java.time.ZonedDateTime::class.java.canonicalName
    )

    override fun map(partyProp: MappedProperty, modes: List<PersistenrPropertyMappingMode>): List<MappedProperty> = listOf(partyProp)
}