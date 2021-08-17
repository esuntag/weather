package esuntag.weather

import org.http4s.HttpApp
import org.http4s.server.Server
import org.http4s.client.Client
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.server.middleware.Logger
import org.http4s.implicits.*
import cats.effect.Async
import cats.effect.kernel.Resource
import com.comcast.ip4s.{Host, Port}

object WeatherServer:
  def makeServer[F[_]](using Async[F])(httpApp: HttpApp[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withPort(Port.fromInt(8080).get)
      .withHostOption(Host.fromString("localhost"))
      .withHttpApp(httpApp)
      .build

  def makeClient[F[_]](using Async[F]): Resource[F, Client[F]] =
    EmberClientBuilder
      .default[F]
      .build

  def runApplication[F[_]](key: String)(using M: Async[F]): F[Nothing] =
    makeClient[F].flatMap(
      client =>
        val weatherAlg = Weather.impl[F](key,client)
        val httpApp = WeatherRoutes.weatherRoutes[F](weatherAlg).orNotFound
        val finalApp = Logger.httpApp(true, true)(httpApp)
        makeServer(httpApp)
    ).use(_ => M.never)