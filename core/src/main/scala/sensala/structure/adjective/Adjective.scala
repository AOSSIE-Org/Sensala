package sensala.structure.adjective

import org.aossie.scavenger.expression._
import org.atnos.eff._
import sensala.structure._

final case class Adjective(word: String) extends Word {
  override def interpret(cont: NLEffE): NLEffE = Eff.pure(Sym(word))
}
