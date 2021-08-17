package esuntag.weather

import cats.effect.Sync
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.dsl.*

object WeatherRoutes:
  def weatherRoutes[F[_]](W: Weather[F])(using F: Sync[F]): HttpRoutes[F] = 
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "weather" :? Lat(lat) +& Lon(lon) =>
        for {
          weather <- W.get(lat,lon)
          resp <- Ok(weather)
        } yield resp
    }

  object Lat extends impl.QueryParamDecoderMatcher[String]("lat")
  object Lon extends impl.QueryParamDecoderMatcher[String]("lon")