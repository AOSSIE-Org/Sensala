package sensala.models

import cats.syntax.functor._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import sensala.models.nl.NL

sealed trait SensalaInterpretMessage

final case class SensalaRunInterpretation(discourse: String) extends SensalaInterpretMessage

final case class StanfordParsed(result: SensalaNode) extends SensalaInterpretMessage

final case class SensalaParsed(result: NL) extends SensalaInterpretMessage

final case class SensalaInterpreted(result: String) extends SensalaInterpretMessage

final case class KeepAliveMsg() extends SensalaInterpretMessage

// Errors
final case class SensalaError(error: String) extends SensalaInterpretMessage

object SensalaInterpretMessage {
  implicit val encoder: Encoder[SensalaInterpretMessage] = Encoder.instance {
    case run @ SensalaRunInterpretation(_)   => run.asJson
    case stanfordParsed @ StanfordParsed(_)  => stanfordParsed.asJson
    case sensalaParsed @ SensalaParsed(_)    => sensalaParsed.asJson
    case interpreted @ SensalaInterpreted(_) => interpreted.asJson
    case keepAlive @ KeepAliveMsg()          => keepAlive.asJson
    case error @ SensalaError(_)             => error.asJson
  }

  implicit val decoder: Decoder[SensalaInterpretMessage] =
    List[Decoder[SensalaInterpretMessage]](
      Decoder[SensalaRunInterpretation].widen,
      Decoder[StanfordParsed].widen,
      Decoder[SensalaParsed].widen,
      Decoder[SensalaInterpreted].widen,
      Decoder[KeepAliveMsg].widen,
      Decoder[SensalaError].widen,
    ).reduceLeft(_ or _)
}
