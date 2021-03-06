package sensala

import cats.Functor
import cats.effect.{ExitCode, IO, IOApp}
import cats.mtl.FunctorRaise
import cats.implicits._
import sensala.normalization.NormalFormConverter
import sensala.postprocessing.PrettyTransformer
import sensala.structure._
import org.aossie.scavenger.expression.formula.True
import org.aossie.scavenger.preprocessing.TPTPClausifier
import org.aossie.scavenger.structure.immutable.AxiomClause
import sensala.shared.effect.Log
import sensala.error.NLError
import sensala.error.NLError.FunctorRaiseNLError
import sensala.interpreter.Interpreter
import sensala.parser.english.EnglishDiscourseParser
import sensala.interpreter.context.{Context, LocalContext}
import sensala.property.{PropertyExtractor, WordNetPropertyExtractor}

object CLI extends IOApp {
  case class Config(discourse: String = "")

  private val parser = new scopt.OptionParser[Config]("sensala") {
    head(
      """
        |Sensala's Command Line Interface
        |
        |
      """.stripMargin
    )

    arg[String]("<discourse>...")
      .optional()
      .action { (v, c) =>
        c.copy(discourse = v)
      }
      .text("interpret <discourse>\n")

    help("help").text("print this usage text")

    note(
      """Example:
        |
        |sensala "John loves Mary"
      """.stripMargin
    )
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val c                     = parser.parse(args, Config()).get
    implicit val log: Log[IO] = Log.log[IO]
    implicit val raiseNLError: FunctorRaiseNLError[IO] = new FunctorRaise[IO, NLError] {
      override val functor: Functor[IO] = Functor[IO]

      override def raise[A](e: NLError): IO[A] =
        throw new RuntimeException(e.toString)
    }
    WordNetPropertyExtractor.create[IO]().flatMap { implicit wordNetPropertyExtractor =>
      implicit val propertyExtractor: PropertyExtractor[IO] = PropertyExtractor()
      implicit val sensalaContext: Context[IO]              = Context.initial
      implicit val sensalaLocalContext: LocalContext[IO]    = LocalContext.empty
      val interpreter                                       = Interpreter[IO]()
      EnglishDiscourseParser.parse(c.discourse) match {
        case Left(error) =>
          Log[IO].error(
            s"""Parsing failed:
               |  $error
            """.stripMargin
          ) >> IO.pure(ExitCode.Error)
        case Right(sentence) =>
          for {
            _ <- Log[IO].info(
                  s"""
                     |Result of sentence parsing:
                     |  $sentence
                """.stripMargin
                )
            lambdaTerm   <- interpreter.interpret(sentence, IO.pure(True))
            context      <- sensalaContext.state.get
            localContext <- sensalaLocalContext.state.get
            _ <- Log[IO].info(
                  s"""
                     |Result of discourse interpretation:
                     |  $lambdaTerm
                     |  ${lambdaTerm.pretty}
                """.stripMargin
                )
            result = NormalFormConverter.normalForm(lambdaTerm)
            _ <- Log[IO].info(
                  s"""
                     |Result of normalization:
                     |  $result
                     |  ${result.pretty}
                """.stripMargin
                )
            prettyTerm = PrettyTransformer.transform(result)
            _ <- Log[IO].info(
                  s"""
                     |Result of applying pretty
                     |  $prettyTerm
                     |  ${prettyTerm.pretty}
                """.stripMargin
                )
            _ <- Log[IO].info(
                  s"""
                     |Context after interpretation:
                     |  ${context.entityProperties.map(_._2.pretty).mkString("\n")}
                """.stripMargin
                )
            cnf = new TPTPClausifier().apply(List((prettyTerm, AxiomClause)))
            _ <- Log[IO].info(
                  s"""
                     |Result of clausification:
                     |${cnf.clauses.mkString("\n")}
                """.stripMargin
                )
          } yield ExitCode.Success
      }
    }
  }
}
