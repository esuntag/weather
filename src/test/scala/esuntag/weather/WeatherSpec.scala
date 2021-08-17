package esuntag.weather

import munit.CatsEffectSuite
import cats.data.Kleisli
import cats.effect.IO
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.client.Client

class HelloWorldSpec extends CatsEffectSuite {
  val sendJson: String = """{"lat":38.9857,"lon":-76.8766,"timezone":"America/New_York","timezone_offset":-14400,"current":{"dt":1629153641,"sunrise":1629109331,"sunset":1629158493,"temp":298.54,"feels_like":299.43,"pressure":1020,"humidity":88,"dew_point":296.41,"uvi":0.1,"clouds":90,"visibility":10000,"wind_speed":0,"wind_deg":0,"weather":[{"id":701,"main":"Mist","description":"mist","icon":"50d"}]},"alerts": [{"sender_name": "NWS Tulsa","event": "Heat Advisory","start": 1597341600,"end": 1597366800,"description": "...HEAT ADVISORY REMAINS IN EFFECT FROM 1 PM THIS AFTERNOON TO\n8 PM CDT THIS EVENING...\n* WHAT...Heat index values of 105 to 109 degrees expected.\n* WHERE...Creek, Okfuskee, Okmulgee, McIntosh, Pittsburg,\nLatimer, Pushmataha, and Choctaw Counties.\n* WHEN...From 1 PM to 8 PM CDT Thursday.\n* IMPACTS...The combination of hot temperatures and high\nhumidity will combine to create a dangerous situation in which\nheat illnesses are possible.","tags":["Extreme temperature value"]}]}"""
  val sampleResponseJson: String = """{"current":{"feels_like":"Hot","weather":[{"description":"mist"}]},"alerts":[{"sender_name":"NWS Tulsa","event":"Heat Advisory","start":1597341600,"end":1597366800,"description":"...HEAT ADVISORY REMAINS IN EFFECT FROM 1 PM THIS AFTERNOON TO\n8 PM CDT THIS EVENING...\n* WHAT...Heat index values of 105 to 109 degrees expected.\n* WHERE...Creek, Okfuskee, Okmulgee, McIntosh, Pittsburg,\nLatimer, Pushmataha, and Choctaw Counties.\n* WHEN...From 1 PM to 8 PM CDT Thursday.\n* IMPACTS...The combination of hot temperatures and high\nhumidity will combine to create a dangerous situation in which\nheat illnesses are possible.","tags":["Extreme temperature value"]}]}"""

  test("HelloWorld returns status code 200") {
    assertIO(retHelloWorld.map(_.status) ,Status.Ok)
  }

  test("HelloWorld returns hello world message") {
    assertIO(retHelloWorld.flatMap(_.as[String]), sampleResponseJson)
  }

  private[this] val retHelloWorld: IO[Response[IO]] = {
    val testApp: HttpApp[IO] = Kleisli.liftF(
      IO(Response(
          Status.Ok,
          HttpVersion.`HTTP/2.0`,
          Headers.empty,
          EntityEncoder[IO, String].toEntity(sendJson).body
      )))
    val testClient: Client[IO] = Client.fromHttpApp(testApp)
    val getHW = Request[IO](Method.GET, uri"/weather?lat=0&lon=0")
    val weather: Weather[IO] = Weather.impl[IO]("fakekey", testClient)
    WeatherRoutes.weatherRoutes(weather).orNotFound(getHW)
  }
}