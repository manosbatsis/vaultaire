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
package com.github.manosbatsis.vaultaire.example.workflow


import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.SingleFinalizedTxOutputConverter
import com.github.manosbatsis.partiture.flow.tx.TransactionBuilderWrapper
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleTypeCheckingResponderTxStrategy
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateResponder
import com.github.manosbatsis.vaultaire.example.contract.BOOK_CONTRACT_ID
import com.github.manosbatsis.vaultaire.example.contract.BookContract
import com.github.manosbatsis.vaultaire.example.generated.BookStateDto
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import java.util.Date


class BookInputConverter : PartitureFlowDelegateBase(), InputConverter<BookStateDto> {
    override fun convert(input: BookStateDto): CallContext {
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addOutputState(input
                        .copy(
                                publisher = input.publisher ?: clientFlow.ourIdentity,
                                published = input.published ?: Date(),
                                linearId = input.linearId ?: UniqueIdentifier()
                        )
                        .toState(), BOOK_CONTRACT_ID)
                .addCommand(BookContract.Send())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

open class BaseBookFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleTypeCheckingResponderTxStrategy(
                BookContract.BookState::class.java)
)

/** Create/publish a bookstate  */
@InitiatingFlow
@StartableByRPC
@VaultaireGenerateResponder(
    value = BaseBookFlowResponder::class,
    comment = "A basic responder for countersigning and listening for finality"
)
class CreateBookFlow(input: BookStateDto) : PartitureFlow<BookStateDto, SignedTransaction>(
        input = input, // Input can be anything
        inputConverter = BookInputConverter(),// Our custom IN converter
        outputConverter = SingleFinalizedTxOutputConverter()) // OUT build-in converter
