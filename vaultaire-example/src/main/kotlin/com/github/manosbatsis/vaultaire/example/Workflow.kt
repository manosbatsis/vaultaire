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
package com.github.manosbatsis.vaultaire.example


import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.SingleFinalizedTxOutputConverter
import com.github.manosbatsis.partiture.flow.tx.TransactionBuilderWrapper
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleTypeCheckingResponderTxStrategy
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

/** Used as flow input, to send a recipient a message */
data class BookMessage(
        val author: Party,
        val title: String,
        val price: BigDecimal,
        val genre: BookContract.BookGenre,
        val editions: Int = 1,
        val linearId: UniqueIdentifier = UniqueIdentifier()
)

class BookInputConverter : PartitureFlowDelegateBase(), InputConverter<BookMessage> {
    override fun convert(input: BookMessage): CallContext {
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addOutputState(
                    BookContract.BookState(
                            linearId = input.linearId,
                            publisher = clientFlow.ourIdentity,
                            author = input.author,
                            editions = input.editions,
                            price = input.price,
                            genre = input.genre,
                            title = input.title),
                    BOOK_CONTRACT_ID)
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
class CreateBookFlow(input: BookMessage) : PartitureFlow<BookMessage, SignedTransaction>(
        input = input, // Input can be anything
        inputConverter = BookInputConverter(),// Our custom IN converter
        outputConverter = SingleFinalizedTxOutputConverter()) // OUT build-in converter


/** A basic responder for countersigning and listening for finality */
@InitiatedBy(CreateBookFlow::class)
class CreateBookFlowResponder(otherPartySession: FlowSession) : BaseBookFlowResponder(otherPartySession)
