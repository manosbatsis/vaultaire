package com.github.manosbatsis.vaultaire.util

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class DummyContract: Contract {

    companion object {
        const val ID = "com.github.manosbatsis.vaultaire.util.DummyContract"
    }

    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }
}
