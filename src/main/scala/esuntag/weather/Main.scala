package esuntag.weather

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp:
  def run(args: List[String]) =
    WeatherServer.runApplication[IO](args.head).as(ExitCode.Success)