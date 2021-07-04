package com.github.manosbatsis.vaultaire.annotation

enum class VaultaireDtoStrategyKeys(val classNameSuffix: String) {
    /** For [ContractState]-based DTOs without any type conversions */
    CORDAPP_LOCAL_DTO("StateDto"),
    /** For [ContractState]-based DTOs with REST or otherwise client-friendly type conversions */
    CORDAPP_CLIENT_DTO("StateClientDto");

    override fun toString(): String {
        return this.classNameSuffix
    }

    companion object {
        fun findFromString(s: String): VaultaireDtoStrategyKeys? {
            val sUpper = s.toUpperCase()
            return VaultaireDtoStrategyKeys.values()
                    .find {
                        it.name.toUpperCase() == sUpper
                                || it.classNameSuffix.toUpperCase() == sUpper
                    }
        }

        fun getFromString(s: String): VaultaireDtoStrategyKeys = findFromString(s)
                ?: error("Could not match input $s to VaultaireDtoStrategyKeys entry")

    }

}