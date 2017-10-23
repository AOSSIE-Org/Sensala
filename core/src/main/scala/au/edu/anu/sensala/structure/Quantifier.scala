package au.edu.anu.sensala.structure

import cats.data.State
import org.aossie.scavenger.expression._
import org.aossie.scavenger.expression.formula._

trait Quantifier extends NounPhrase

case class ForallQuantifier(nounPhrase: NounPhrase) extends Quantifier {
  override def interpret: CState = for {
    f <- bindFreeSym
    p <- nounPhrase.interpret
    x <- bindFreeSym
    _ <- State.modify[Context](_.extend(x))
    q <- bindFreeSym
    _ <- State.modify[Context](_.deleteReferent(x))
  } yield Abs(q, i, Abs(f, i, And(All(x, i, Neg(App(App(p, x), Neg(App(App(q, x), True))))), f)))
}

case class ExistentialQuantifier(commonNoun: CommonNoun) extends Quantifier {
  override def interpret: CState = for {
    f <- bindFreeSym
    p <- commonNoun.interpret
    x <- bindFreeSym
    _ <- State.modify[Context](_.extend(x))
    q <- bindFreeSym
  } yield Abs(q, i, Abs(f, i, Ex(x, i, App(App(p, x), App(App(q, x), f)))))
}

