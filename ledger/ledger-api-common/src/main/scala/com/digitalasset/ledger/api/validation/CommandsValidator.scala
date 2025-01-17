// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.digitalasset.ledger.api.validation

import com.digitalasset.api.util.TimestampConversion
import com.digitalasset.daml.lf.command._
import com.digitalasset.daml.lf.data._
import com.digitalasset.daml.lf.value.Value.ValueUnit
import com.digitalasset.ledger.api.domain
import com.digitalasset.ledger.api.v1.commands.Command.Command.{
  Create => ProtoCreate,
  CreateAndExercise => ProtoCreateAndExercise,
  Empty => ProtoEmpty,
  Exercise => ProtoExercise
}
import com.digitalasset.ledger.api.v1.commands.{Command => ProtoCommand, Commands => ProtoCommands}
import com.digitalasset.ledger.api.v1.value.Value.Sum
import com.digitalasset.ledger.api.v1.value.{
  Identifier,
  RecordField,
  Value,
  List => ApiList,
  Map => ApiMap,
  Variant => ApiVariant,
  Enum => ApiEnum
}
import com.digitalasset.daml.lf.value.{Value => Lf}
import com.digitalasset.ledger.api.domain.LedgerId
import com.digitalasset.platform.common.PlatformTypes.asVersionedValueOrThrow
import com.digitalasset.platform.server.api.validation.ErrorFactories._
import com.digitalasset.platform.server.api.validation.FieldValidations.{requirePresence, _}
import com.digitalasset.platform.server.api.validation.IdentifierResolver
import io.grpc.StatusRuntimeException
import scalaz.syntax.tag._

import scala.collection.immutable

final class CommandsValidator(ledgerId: LedgerId, identifierResolver: IdentifierResolver) {

  def validateCommands(commands: ProtoCommands): Either[StatusRuntimeException, domain.Commands] =
    for {
      cmdLegerId <- requireLedgerString(commands.ledgerId, "ledger_id")
      ledgerId <- matchLedgerId(ledgerId)(LedgerId(cmdLegerId))
      workflowId <- if (commands.workflowId.isEmpty) Right(None)
      else requireLedgerString(commands.workflowId).map(x => Some(domain.WorkflowId(x)))
      appId <- requireLedgerString(commands.applicationId, "application_id")
        .map(domain.ApplicationId(_))
      commandId <- requireLedgerString(commands.commandId, "command_id").map(domain.CommandId(_))
      submitter <- requireParty(commands.party, "party")
      let <- requirePresence(commands.ledgerEffectiveTime, "ledger_effective_time")
      ledgerEffectiveTime = TimestampConversion.toInstant(let)
      mrt <- requirePresence(commands.maximumRecordTime, "maximum_record_time")
      validatedCommands <- validateInnerCommands(commands.commands, submitter)
      ledgerEffectiveTimestamp <- Time.Timestamp
        .fromInstant(ledgerEffectiveTime)
        .left
        .map(invalidField(_, "ledger_effective_time"))
    } yield
      domain.Commands(
        ledgerId,
        workflowId,
        appId,
        commandId,
        submitter,
        ledgerEffectiveTime,
        TimestampConversion.toInstant(mrt),
        Commands(
          ImmArray(validatedCommands),
          ledgerEffectiveTimestamp,
          workflowId.fold("")(_.unwrap)),
      )

  private def validateInnerCommands(
      commands: Seq[ProtoCommand],
      submitter: Ref.Party
  ): Either[StatusRuntimeException, immutable.Seq[Command]] =
    commands.foldLeft[Either[StatusRuntimeException, Vector[Command]]](
      Right(Vector.empty[Command]))((commandz, command) => {
      for {
        validatedInnerCommands <- commandz
        validatedInnerCommand <- validateInnerCommand(command.command, submitter)
      } yield validatedInnerCommands :+ validatedInnerCommand
    })

  private def validateInnerCommand(
      command: ProtoCommand.Command,
      submitter: Ref.Party): Either[StatusRuntimeException, Command] =
    command match {
      case c: ProtoCreate =>
        for {
          templateId <- requirePresence(c.value.templateId, "template_id")
          validatedTemplateId <- identifierResolver.resolveIdentifier(templateId)
          createArguments <- requirePresence(c.value.createArguments, "create_arguments")
          recordId <- validateOptionalIdentifier(createArguments.recordId)
          validatedRecordField <- validateRecordFields(createArguments.fields)
        } yield
          CreateCommand(
            templateId = validatedTemplateId,
            argument = asVersionedValueOrThrow(Lf.ValueRecord(recordId, validatedRecordField)))

      case e: ProtoExercise =>
        for {
          templateId <- requirePresence(e.value.templateId, "template_id")
          validatedTemplateId <- identifierResolver.resolveIdentifier(templateId)
          contractId <- requireLedgerString(e.value.contractId, "contract_id")
          choice <- requireName(e.value.choice, "choice")
          value <- requirePresence(e.value.choiceArgument, "value")
          validatedValue <- validateValue(value)
        } yield
          ExerciseCommand(
            templateId = validatedTemplateId,
            contractId = contractId,
            choiceId = choice,
            submitter = submitter,
            argument = asVersionedValueOrThrow(validatedValue))
      case ce: ProtoCreateAndExercise =>
        for {
          templateId <- requirePresence(ce.value.templateId, "template_id")
          validatedTemplateId <- identifierResolver.resolveIdentifier(templateId)
          createArguments <- requirePresence(ce.value.createArguments, "create_arguments")
          recordId <- validateOptionalIdentifier(createArguments.recordId)
          validatedRecordField <- validateRecordFields(createArguments.fields)
          choice <- requireName(ce.value.choice, "choice")
          value <- requirePresence(ce.value.choiceArgument, "value")
          validatedChoiceArgument <- validateValue(value)
        } yield
          CreateAndExerciseCommand(
            templateId = validatedTemplateId,
            createArgument = asVersionedValueOrThrow(Lf.ValueRecord(recordId, validatedRecordField)),
            choiceId = choice,
            choiceArgument = asVersionedValueOrThrow(validatedChoiceArgument),
            submitter = submitter
          )
      case ProtoEmpty =>
        Left(missingField("command"))
    }

  private def validateRecordFields(recordFields: Seq[RecordField])
    : Either[StatusRuntimeException, ImmArray[(Option[Ref.Name], domain.Value)]] =
    recordFields
      .foldLeft[Either[StatusRuntimeException, BackStack[(Option[Ref.Name], domain.Value)]]](
        Right(BackStack.empty))((acc, rf) => {
        for {
          fields <- acc
          v <- requirePresence(rf.value, "value")
          value <- validateValue(v)
          label <- if (rf.label.isEmpty) Right(None) else requireIdentifier(rf.label).map(Some(_))
        } yield fields :+ label -> value
      })
      .map(_.toImmArray)

  def validateValue(value: Value): Either[StatusRuntimeException, domain.Value] = value.sum match {
    case Sum.ContractId(cId) =>
      Ref.ContractIdString
        .fromString(cId)
        .left
        .map(invalidArgument)
        .map(coid => Lf.ValueContractId(Lf.AbsoluteContractId(coid)))
    case Sum.Decimal(value) =>
      Decimal.fromString(value).left.map(invalidArgument).map(Lf.ValueDecimal)

    case Sum.Party(party) =>
      Ref.Party.fromString(party).left.map(invalidArgument).map(Lf.ValueParty)
    case Sum.Bool(b) => Right(Lf.ValueBool(b))
    case Sum.Timestamp(micros) =>
      Time.Timestamp.fromLong(micros).left.map(invalidArgument).map(Lf.ValueTimestamp)
    case Sum.Date(days) =>
      Time.Date.fromDaysSinceEpoch(days).left.map(invalidArgument).map(Lf.ValueDate)
    case Sum.Text(text) => Right(Lf.ValueText(text))
    case Sum.Int64(value) => Right(Lf.ValueInt64(value))
    case Sum.Record(rec) =>
      for {
        recId <- validateOptionalIdentifier(rec.recordId)
        fields <- validateRecordFields(rec.fields)
      } yield Lf.ValueRecord(recId, fields)
    case Sum.Variant(ApiVariant(variantId, constructor, value)) =>
      for {
        validatedVariantId <- validateOptionalIdentifier(variantId)
        validatedConstructor <- requireName(constructor, "constructor")
        v <- requirePresence(value, "value")
        validatedValue <- validateValue(v)
      } yield Lf.ValueVariant(validatedVariantId, validatedConstructor, validatedValue)
    case Sum.Enum(ApiEnum(enumId, value)) =>
      for {
        validatedEnumId <- validateOptionalIdentifier(enumId)
        validatedValue <- requireName(value, "value")
      } yield Lf.ValueEnum(validatedEnumId, validatedValue)
    case Sum.List(ApiList(elems)) =>
      elems
        .foldLeft[Either[StatusRuntimeException, BackStack[domain.Value]]](Right(BackStack.empty))(
          (valuesE, v) =>
            for {
              values <- valuesE
              validatedValue <- validateValue(v)
            } yield values :+ validatedValue)
        .map(elements => Lf.ValueList(FrontStack(elements.toImmArray)))
    case _: Sum.Unit => Right(ValueUnit)
    case Sum.Optional(o) =>
      o.value.fold[Either[StatusRuntimeException, domain.Value]](Right(Lf.ValueOptional(None)))(
        validateValue(_).map(v => Lf.ValueOptional(Some(v))))
    case Sum.Map(m) =>
      val entries = m.entries
        .foldLeft[Either[StatusRuntimeException, FrontStack[(String, domain.Value)]]](
          Right(FrontStack.empty)) {
          case (acc, ApiMap.Entry(key, value0)) =>
            for {
              tail <- acc
              v <- requirePresence(value0, "value")
              validatedValue <- validateValue(v)
            } yield (key -> validatedValue) +: tail
        }

      for {
        list <- entries
        map <- SortedLookupList.fromImmArray(list.toImmArray).left.map(invalidArgument)
      } yield Lf.ValueMap(map)

    case Sum.Empty => Left(missingField("value"))
  }

  private def validateOptionalIdentifier(
      variantIdO: Option[Identifier]): Either[StatusRuntimeException, Option[Ref.Identifier]] = {
    variantIdO
      .map { variantId =>
        identifierResolver.resolveIdentifier(variantId).map(Some.apply)
      }
      .getOrElse(Right(None))
  }

}
