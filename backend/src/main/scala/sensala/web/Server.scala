package sensala.web

import cats.Functor
import cats.effect._
import cats.implicits._
import cats.mtl.FunctorRaise
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.implicits._
import scopt.OParser
import sensala.shared.effect.Log
import sensala.error.NLError
import sensala.error.NLError.FunctorRaiseNLError
import sensala.interpreter.Interpreter
import sensala.interpreter.context.{Context, LocalContext}
import sensala.property.{PropertyExtractor, WordNetPropertyExtractor}

object Server extends IOApp {
  final case class Config(
    host: String = "localhost",
    port: Int = 8080
  )

  val builder = OParser.builder[Config]
  val parser = {
    import builder._
    OParser.sequence(
      programName("sensala"),
      head("sensala", "0.1"),
      opt[Int]('p', "port").action((port, c) => c.copy(port = port)),
      opt[String]('h', "host").action((host, c) => c.copy(host = host))
    )
  }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val log: Log[IO] = Log.log
    OParser.parse(parser, args, Config()) match {
      case Some(config) =>
        implicit val raiseNLError: FunctorRaiseNLError[IO] = new FunctorRaise[IO, NLError] {
          override val functor: Functor[IO] = Functor[IO]

          override def raise[A](e: NLError): IO[A] =
            throw new RuntimeException(e.toString)
        }
        implicit val sensalaContext: Context[IO]           = Context.initial
        implicit val sensalaLocalContext: LocalContext[IO] = LocalContext.empty
        WordNetPropertyExtractor.create[IO]().flatMap { implicit wordNetPropertyExtractor =>
          implicit val propertyExtractor: PropertyExtractor[IO] = PropertyExtractor()
          implicit val interpreter: Interpreter[IO]             = Interpreter()
          val applicationService                                = ApplicationService[IO]()
          val webjarService                                     = WebjarService[IO]()
          val staticFileService                                 = StaticFileService[IO]()
          val httpApp = Router[IO](
            "/assets" -> staticFileService.staticFiles,
            "/assets" -> webjarService.webjars,
            "/"       -> applicationService.application
          ).orNotFound
          val serverBuilder =
            BlazeServerBuilder[IO].bindHttp(config.port, config.host).withHttpApp(httpApp)
          serverBuilder.serve.compile.drain.as(ExitCode.Success)
        }
      case None =>
        log.error("Invalid arguments").as(ExitCode.Error)
    }
  }
}
