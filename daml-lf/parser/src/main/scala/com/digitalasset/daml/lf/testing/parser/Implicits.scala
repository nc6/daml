// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.digitalasset.daml.lf.testing.parser

import com.digitalasset.daml.lf.data.{Decimal, Ref}
import com.digitalasset.daml.lf.language.Ast.{Expr, Kind, Package, Type}

object Implicits {

  implicit class SyntaxHelper(val sc: StringContext) extends AnyVal {
    def k(args: Any*): Kind = interpolate(KindParser.kind)(args)

    def t(args: Any*): Type = interpolate(TypeParser.typ)(args)

    def e(args: Any*): Expr = interpolate(ExprParser.expr)(args)

    def p(args: Any*): Package = interpolate(ModParser.pkg)(args)

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def n(args: Any*): Ref.Name =
      Ref.Name.assertFromString(sc.standardInterpolator(identity, args.map(prettyPrint)))

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    private def interpolate[T](p: Parsers.Parser[T])(args: Seq[Any]): T =
      Parsers.parseAll(Parsers.phrase(p), sc.standardInterpolator(identity, args.map(prettyPrint)))
  }

  private def toString(x: BigDecimal) =
    Decimal.toString(Decimal.assertFromBigDecimal(x))

  private def prettyPrint(x: Any): String =
    x match {
      case d: BigDecimal => toString(d)
      case d: Float => toString(d.toDouble)
      case d: Double => toString(d)
      case other: Any => other.toString
    }
}
