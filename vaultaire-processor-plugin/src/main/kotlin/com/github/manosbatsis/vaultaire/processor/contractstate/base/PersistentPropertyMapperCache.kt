package com.github.manosbatsis.vaultaire.processor.contractstate.base

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

class PersistentPropertyMapperCache(
        delegate: BaseStateMembersStrategy
): ProcessingEnvironmentAware by delegate{
    companion object{
        val buildInMappers = listOf(
                ::AccountPartyPersistentPropertyMapper,
                ::PartyPersistentPropertyMapper,
                ::CordaX500NamePersistentPropertyMapper,
                ::LinearPointerPersistentPropertyMapper,
                ::IdentityPersistentPropertyMapper
        )
    }

    private val mappings: Map<String, PersistentPropertyMapper<*>>

    init {
        mappings = buildInMappers.map { creator ->
            val mapper = creator(delegate)
            mapper.supportedTypes().map { it to mapper }
        }.flatten().toMap()
    }

    fun toPersistentProperties(mappedProperty: MappedProperty) =
            getMapper(mappedProperty)?.map(mappedProperty)
                    ?: emptyList()

    fun getMapper(mappedProperty: MappedProperty): PersistentPropertyMapper<*>?{
        return getMapper(mappedProperty.variableElement)
                ?: getMapper(mappedProperty.propertyType)
    }

    fun getMapper(typeName: TypeName): PersistentPropertyMapper<*>?{
        return getMapper(typeName.copy(nullable = false).toString())
    }

    fun getMapper(variableElement: VariableElement): PersistentPropertyMapper<*>?{
        return getMapper(variableElement.asType())
                ?: getMapper(variableElement.asKotlinTypeName())
    }

    fun getMapper(type: TypeMirror): PersistentPropertyMapper<*>? {
        return getMapper(type.asTypeName())
                ?: getMapper(type.asKotlinTypeName())
                ?: getMapper(type.asTypeElement())

    }

    fun getMapper(typeElement: TypeElement): PersistentPropertyMapper<*>? {
        return getMapper(typeElement.asClassName())
    }

    fun getMapper(name: Name): PersistentPropertyMapper<*>? {
        return getMapper("$name")
    }

    fun getMapper(typeHint: String): PersistentPropertyMapper<*>? = mappings[typeHint]
}