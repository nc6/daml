// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.digitalasset.daml.lf.iface

import scalaz.std.map._
import scalaz.std.tuple._
import scalaz.syntax.applicative.^
import scalaz.syntax.traverse._
import scalaz.{Applicative, Bifunctor, Bitraverse, Functor, Traverse}
import java.{util => j}

import com.digitalasset.daml.lf.data.ImmArray.ImmArraySeq
import com.digitalasset.daml.lf.data.Ref

import scala.language.higherKinds
import scala.collection.JavaConverters._

case class DefDataType[+RF, +VF](typeVars: ImmArraySeq[Ref.Name], dataType: DataType[RF, VF]) {
  def bimap[C, D](f: RF => C, g: VF => D): DefDataType[C, D] =
    Bifunctor[DefDataType].bimap(this)(f, g)

  def getTypeVars: j.List[_ <: String] = typeVars.asJava
}

object DefDataType {

  /** Alias for application to [[Type]]. Note that FWT stands for "Field with
    * type", because before we parametrized over both the field and the type,
    * while now we only parametrize over the type.
    */
  type FWT = DefDataType[Type, Type]

  implicit val `DDT bitraverse`: Bitraverse[DefDataType] =
    new Bitraverse[DefDataType] {
      override def bitraverseImpl[G[_]: Applicative, A, B, C, D](
          fab: DefDataType[A, B])(f: A => G[C], g: B => G[D]): G[DefDataType[C, D]] = {
        Applicative[G].map(Bitraverse[DataType].bitraverse(fab.dataType)(f)(g))(dataTyp =>
          DefDataType(fab.typeVars, dataTyp))
      }
    }
}

sealed trait DataType[+RT, +VT] extends Product with Serializable {
  def bimap[C, D](f: RT => C, g: VT => D): DataType[C, D] =
    Bifunctor[DataType].bimap(this)(f, g)
}

object DataType {

  /** Alias for application to [[Type]]. Note that FWT stands for "Field with
    * type", because before we parametrized over both the field and the type,
    * while now we only parametrize over the type.
    */
  type FWT = DataType[Type, Type]

  // While this instance appears to overlap the subclasses' traversals,
  // naturality holds with respect to those instances and this one, so there is
  // no risk of confusion.
  implicit val `DT bitraverse`: Bitraverse[DataType] =
    new Bitraverse[DataType] {
      override def bitraverseImpl[G[_]: Applicative, A, B, C, D](
          fab: DataType[A, B])(f: A => G[C], g: B => G[D]): G[DataType[C, D]] =
        fab match {
          case r @ Record(_) =>
            Traverse[Record].traverse(r)(f).widen
          case v @ Variant(_) =>
            Traverse[Variant].traverse(v)(g).widen
          case e @ Enum(_) =>
            Applicative[G].pure(e)
        }
    }

  sealed trait GetFields[+A] {
    def fields: ImmArraySeq[(Ref.Name, A)]
    final def getFields: j.List[_ <: (String, A)] = fields.asJava
  }
}

// Record TypeDecl`s have an object generated for them in their own file
final case class Record[+RT](fields: ImmArraySeq[(Ref.Name, RT)])
    extends DataType[RT, Nothing]
    with DataType.GetFields[RT] {

  /** Widen to DataType, in Java. */
  def asDataType[PRT >: RT, VT]: DataType[PRT, VT] = this
}

object Record extends FWTLike[Record] {
  implicit val `R traverse`: Traverse[Record] =
    new Traverse[Record] {
      override def traverseImpl[G[_]: Applicative, A, B](fa: Record[A])(
          f: A => G[B]): G[Record[B]] =
        Applicative[G].map(fa.fields traverse (_ traverse f))(bs => fa.copy(fields = bs))
    }
}

// Variant TypeDecl`s have an object generated for them in their own file
final case class Variant[+VT](fields: ImmArraySeq[(Ref.Name, VT)])
    extends DataType[Nothing, VT]
    with DataType.GetFields[VT] {

  /** Widen to DataType, in Java. */
  def asDataType[RT, PVT >: VT]: DataType[RT, PVT] = this
}

object Variant extends FWTLike[Variant] {
  implicit val `V traverse`: Traverse[Variant] =
    new Traverse[Variant] {
      override def traverseImpl[G[_]: Applicative, A, B](fa: Variant[A])(
          f: A => G[B]): G[Variant[B]] =
        Applicative[G].map(fa.fields traverse (_ traverse f))(bs => fa.copy(fields = bs))
    }
}

final case class Enum(constructors: ImmArraySeq[Ref.Name]) extends DataType[Nothing, Nothing] {

  /** Widen to DataType, in Java. */
  def asDataType[RT, PVT]: DataType[RT, PVT] = this
}

final case class DefTemplate[+Ty](choices: Map[Ref.Name, TemplateChoice[Ty]]) {
  def map[B](f: Ty => B): DefTemplate[B] = Functor[DefTemplate].map(this)(f)

  def getChoices: j.Map[Ref.ChoiceName, _ <: TemplateChoice[Ty]] =
    choices.asJava
}

object DefTemplate {
  type FWT = DefTemplate[Type]

  implicit val `TemplateDecl traverse`: Traverse[DefTemplate] =
    new Traverse[DefTemplate] {
      override def traverseImpl[G[_]: Applicative, A, B](fab: DefTemplate[A])(
          f: A => G[B]): G[DefTemplate[B]] = {
        Applicative[G].map(fab.choices traverse (_ traverse f))(choices =>
          fab.copy(choices = choices))
      }
    }

}

final case class TemplateChoice[+Ty](param: Ty, consuming: Boolean, returnType: Ty) {
  def map[C](f: Ty => C): TemplateChoice[C] =
    Functor[TemplateChoice].map(this)(f)
}

object TemplateChoice {
  type FWT = TemplateChoice[Type]

  implicit val `Choice traverse`: Traverse[TemplateChoice] = new Traverse[TemplateChoice] {
    override def traverseImpl[G[_]: Applicative, A, B](fa: TemplateChoice[A])(
        f: A => G[B]): G[TemplateChoice[B]] =
      ^(f(fa.param), f(fa.returnType)) { (param, returnType) =>
        fa.copy(param = param, returnType = returnType)
      }
  }
}

/** Add aliases to companions. */
sealed abstract class FWTLike[F[+ _]] {

  /** Alias for application to [[Type]]. Note that FWT stands for "Field with
    * type", because before we parametrized over both the field and the type,
    * while now we only parametrize over the type.
    */
  type FWT = F[Type]
}
