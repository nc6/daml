// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.participant.state.index.v1

import com.daml.ledger.participant.state.v1.Party
import com.digitalasset.daml.lf.value.Value
import com.digitalasset.daml.lf.value.Value.{AbsoluteContractId, ContractInst}

import scala.concurrent.Future

/**
  * Meant be used for optimistic contract lookups before command submission.
  */
trait ContractStore {
  def lookupActiveContract(
      submitter: Party,
      contractId: AbsoluteContractId
  ): Future[Option[ContractInst[Value.VersionedValue[AbsoluteContractId]]]]

}