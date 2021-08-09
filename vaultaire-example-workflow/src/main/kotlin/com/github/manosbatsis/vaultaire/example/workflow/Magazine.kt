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


import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.OutputConverter
import com.github.manosbatsis.partiture.flow.io.output.TypedOutputStatesConverter
import com.github.manosbatsis.partiture.flow.tx.TransactionBuilderWrapper
import com.github.manosbatsis.partiture.flow.tx.initiating.SimpleTxStrategy
import com.github.manosbatsis.partiture.flow.tx.initiating.TxStrategy
import com.github.manosbatsis.partiture.flow.tx.initiating.TxStrategyExecutionException
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleTypeCheckingResponderTxStrategy
import com.github.manosbatsis.partiture.flow.util.IdentitySyncMode.SKIP
import com.github.manosbatsis.vaultaire.annotation.VaultaireFlowResponder
import com.github.manosbatsis.vaultaire.example.contract.support.AbstractPublicationContract.Commands
import com.github.manosbatsis.vaultaire.example.contract.MAGAZINE_CONTRACT_ID
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract
import com.github.manosbatsis.vaultaire.example.contract.MagazineState
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoService
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.SignedTransaction.SignaturesMissingException
import java.security.PublicKey

/* TODO
class StateClientDtoInputConverter<D: VaultaireAccountsAwareStateClientDto<*>> :
        PartitureFlowDelegateBase(),
        InputConverter<D>{
    override fun convert(input: D): CallContext {
        val stateService = MagazineStateService(clientFlow.serviceHub)
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addOutputState(input.toTargetType(), MAGAZINE_CONTRACT_ID)
                .addCommand(MagazineContract.Commands.Create())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

 */
class CreateMagazineInputConverter :
        PartitureFlowDelegateBase(),
        InputConverter<MagazineStateClientDto> {
    @Suspendable
    override fun convert(input: MagazineStateClientDto): CallContext {
        val stateService = MagazineStateService(clientFlow.serviceHub)
        val contractState = input.toTargetType(stateService)
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addOutputState(contractState, MAGAZINE_CONTRACT_ID)
                .addCommand(Commands.Create())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}


class UpdateMagazineInputConverter :
        PartitureFlowDelegateBase(),
        InputConverter<MagazineStateClientDto> {
    @Suspendable
    override fun convert(input: MagazineStateClientDto): CallContext {
        val stateService = MagazineStateService(clientFlow.serviceHub)
        // Load existing state
        val existing = stateService.getByLinearId(input.linearId!!)
        val updated = input.toPatched(existing.state.data, stateService)
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addInputState(existing)
                .addOutputState(updated, MAGAZINE_CONTRACT_ID)
                .addCommand(Commands.Update())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

class DeleteMagazineInputConverter : PartitureFlowDelegateBase(), InputConverter<MagazineStateDto> {
    override fun convert(input: MagazineStateDto): CallContext {
        val stateService = MagazineStateService(clientFlow.serviceHub)
        // Load existing state
        val existing = stateService.getByLinearId(input.linearId!!)
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addInputState(existing)
                .addCommand(Commands.Update())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

open class BaseMagazineFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleTypeCheckingResponderTxStrategy(
                MagazineState::class.java)
)

abstract class BaseMagazineFlow<IN, OUT>(
        input: IN,
        txStrategy: TxStrategy = SimpleTxStrategy(),
        inputConverter: InputConverter<IN>?,
        outputConverter: OutputConverter<OUT>?
) : PartitureFlow<IN, OUT>(
        input, txStrategy, inputConverter, outputConverter, SKIP
) {

    /** Ignore Corda Account keys, use the defaultnode legal identity
    @Suspendable
    override fun signInitialTransaction(
    transactionBuilder: TransactionBuilder,
    signingPubKeys: Iterable<PublicKey>
    ): SignedTransaction? {
    serviceHub.identityService.wellKnownPartyFromAnonymous(AnonymousParty())
    return serviceHub.signInitialTransaction(transactionBuilder)
    }*/

    /** Assume all participants are accounts and need a [FlowSession] */
    @Suspendable
    override fun createFlowSessions(
            counterParties: Iterable<AbstractParty>
    ): Set<FlowSession> {
        val parties = (counterParties)
        val sessions = mutableSetOf<FlowSession>()
        for (party in parties) {
            sessions.add(initiateFlow(party))
        }
        return sessions
    }

    @Suspendable
    override fun ourParticipatingKeys(ourParties: List<AbstractParty>): Iterable<PublicKey> {
        val identityService = serviceHub.identityService
        val nodeKeys = ourParties
                .filterIsInstance(AnonymousParty::class.java)
                .map { identityService.requireWellKnownPartyFromAnonymous(AnonymousParty(it.owningKey)) }
                .map { it.owningKey }
        val accountKeys = ourParties.map { it.owningKey }
        return nodeKeys + accountKeys

    }

    override fun handleFailedTxStrategy(e: TxStrategyExecutionException) {

        //super.handleFailedTxStrategy(e)
        val inner = e.cause
        if (inner is SignaturesMissingException) {
            logger.error("Strategy errored: ${this.javaClass.simpleName} ")
            val accountInfoService = AccountInfoService(serviceHub)
            inner.missing.forEach {
                val accountInfoDto = accountInfoService.toAccountInfoDtoOrNull(it)
                val accountInfo = accountInfoService.findAccountInfo(accountInfoDto)
            }
            inner.printStackTrace()
            throw e
        } else super.handleFailedTxStrategy(e)
    }
}

/** Create/publish a magazinestate  */
@InitiatingFlow
@StartableByRPC
@VaultaireFlowResponder(
        value = BaseMagazineFlowResponder::class,
        comment = "A basic responder for countersigning and listening for finality"
)
class CreateMagazineFlow(input: MagazineStateClientDto) :
        BaseMagazineFlow<MagazineStateClientDto, List<MagazineState>>(
                input = input, // Input can be anything
                inputConverter = CreateMagazineInputConverter(),// Our custom IN converter
                outputConverter = TypedOutputStatesConverter(MagazineState::class.java)
        )

/** Create/publish a magazinestate  */
@InitiatingFlow
@StartableByRPC
@VaultaireFlowResponder(
        value = BaseMagazineFlowResponder::class,
        comment = "A basic responder for countersigning and listening for finality"
)
class UpdateMagazineFlow(input: MagazineStateClientDto) :
        BaseMagazineFlow<MagazineStateClientDto, List<MagazineState>>(
                input = input, // Input can be anything
                inputConverter = UpdateMagazineInputConverter(),// Our custom IN converter
                outputConverter = TypedOutputStatesConverter(MagazineState::class.java)// OUT build-in converter
        )


/** Create/publish a magazinestate  */
@InitiatingFlow
@StartableByRPC
@VaultaireFlowResponder(
        value = BaseMagazineFlowResponder::class,
        comment = "A basic responder for countersigning and listening for finality"
)
class DeleteMagazineFlow(input: MagazineStateDto) :
        BaseMagazineFlow<MagazineStateDto, List<MagazineState>>(
                input = input, // Input can be anything
                inputConverter = DeleteMagazineInputConverter(),// Our custom IN converter
                outputConverter = TypedOutputStatesConverter(MagazineState::class.java)) // OUT build-in converter

