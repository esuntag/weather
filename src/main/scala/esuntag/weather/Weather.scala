package esuntag.weather

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.circe.*
import org.http4s.Method.*
import cats.effect.kernel.Sync
import java.time.LocalDateTime
import java.time.Instant
import cats.MonadError
import io.circe.Printer

enum TemperatureFeel:
  case Hot
  case Moderate
  case Cold
object TemperatureFeel:
  def fromDouble(temp: Double): TemperatureFeel =
    if (temp < 50) then Cold
    else if (temp < 85) then Moderate
    else Hot

final case class Alert(sender_name: String, event: String, start: Instant, end: Instant, description: String, tags: List[String])
final case class Description(description: String)
final case class Current(feels_like: TemperatureFeel, weather: List[Description])
final case class WeatherData(current: Current, alerts: Option[List[Alert]])
object WeatherData:
  given Decoder[Instant] = Decoder[Long].map(Instant.ofEpochMilli)
  given Decoder[TemperatureFeel] = Decoder[Double].map(TemperatureFeel.fromDouble).or(Decoder[String].map(TemperatureFeel.valueOf))
  given Decoder[Alert] = deriveDecoder
  given Decoder[Description] = deriveDecoder
  given Decoder[Current] = deriveDecoder

  given Decoder[WeatherData] = deriveDecoder[WeatherData]
  given [F[_]](using m: Concurrent[F]): EntityDecoder[F, WeatherData] = jsonOf

  given Encoder[Instant] = Encoder[Long].contramap(_.toEpochMilli)
  given Encoder[TemperatureFeel] = Encoder[String].contramap(_.productPrefix)
  given Encoder[Alert] = deriveEncoder
  given Encoder[Description] = deriveEncoder
  given Encoder[Current] = deriveEncoder

  given Encoder[WeatherData] = deriveEncoder[WeatherData].mapJson(_.deepDropNullValues)
  given [F[_]]: EntityEncoder[F, WeatherData] = jsonEncoderOf

trait Weather[F[_]]:
  def get(lat: String, lon: String): F[WeatherData]

object Weather:
  def apply[F[_]](using ev: Weather[F]): Weather[F] = ev

  val oneCallQuery: Uri =
    uri"https://api.openweathermap.org/data/2.5/onecall"
      .withQueryParams[String, String](Map(
        "units" -> "imperial",
        "exclude" -> "minutely,hourly,daily"
      ))

  final case class WeatherError(e: Throwable) extends RuntimeException

  def impl[F[_]](key: String, C: Client[F])(using Concurrent[F]): Weather[F] =
    new Weather[F]:
      val dsl = new Http4sClientDsl[F]{}
      import dsl._
      def get(lat: String, lon: String): F[WeatherData] =
        val finalQuery: Uri = oneCallQuery.withQueryParams[String,String](Map(
          "lat" -> lat,
          "lon" -> lon,
          "appid" -> key
        ))
        C.expect[WeatherData](GET(finalQuery))
          .adaptError{case t => WeatherError(t)}