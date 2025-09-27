

package mainSc

import zio._
import zio.http._
import zio.json._

import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.io.{BufferedSource, Source}
import java.io.InputStream
import scala.collection.mutable.ArrayBuffer


case class MyRecord(isHit: Boolean ,X: Int, Y:Float,R:Int,OnTime:String ,WorkTime:Long )

object MyRecord {
  implicit val decoder: JsonDecoder[MyRecord] = DeriveJsonDecoder.gen[MyRecord]
  implicit val encoder: JsonEncoder[MyRecord] =
    DeriveJsonEncoder.gen[MyRecord]
}

case class ReqRes(X: Int, Y:Float,R:Int)

object ReqRes {
  implicit val decoder: JsonDecoder[ReqRes] = DeriveJsonDecoder.gen[ReqRes]
  implicit val encoder: JsonEncoder[ReqRes] =
    DeriveJsonEncoder.gen[ReqRes]
}

object MainApp extends ZIOAppDefault {

  val startTime = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.run(Clock.nanoTime).getOrThrow()
  }

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  var AppData  = ArrayBuffer[MyRecord]()

  def checkInside(req: ReqRes ): Boolean = {

    if (req.X >= -req.R && req.X <= 0 && req.Y >= 0 && req.Y <= req.R) {
      return req.X * req.X + req.Y * req.Y <= req.R * req.R
    }


    if (req.X >= -req.R && req.X <= 0 && req.Y < 0 && req.Y >= -req.R / 2) {
      return true
    }


    if (req.X > 0 && req.X <= req.R && req.Y <= 0 && req.Y >= -req.R) {

      return req.Y >= req.X - req.R
    }

    false
  }



  def readResource(path: String): Option[String] = {

    val classLoader = getClass.getClassLoader

    val inputStream: InputStream = classLoader.getResourceAsStream(path)

    try {

      val source: BufferedSource = Source.fromInputStream(inputStream, "UTF-8")
      val source2: BufferedSource = Source.fromInputStream(inputStream, "UTF-8")

      try {
        Some(source.mkString)
      } finally {
        source.close()
      }
    } catch {
      case e: NullPointerException =>
        println(s"Не удалось найти ресурс: $path")
        None
    }
  }


  def readResourceByte(path: String): Option[Array[Byte]] = {

    val classLoader = getClass.getClassLoader

    val inputStream: InputStream = classLoader.getResourceAsStream(path)

    try {

      val sourceD = inputStream.readAllBytes()

      try {
        Some(sourceD)
      } finally {
        inputStream.close()
      }
    } catch {
      case e: NullPointerException =>
        println(s"Не удалось найти ресурс: $path")
        None
    }
  }


  val mainPage = readResource("static/index.html") match {
    case Some(s) => s
    case _ => "404"
  }
  val testRes = Response.html("TEST")
  val mainRes = Response(Status.Ok,Headers.empty,Body.fromString(mainPage))

  val image1 = readResourceByte("images/gol.jpg") match {
    case Some(im) => im
    case _ => Array[Byte]()
  }
  val image1Res = Response(Status.Ok,Headers(
    Header.ContentType(MediaType.image.jpeg)
  ) , Body.fromArray(image1))


  val image2 = readResourceByte("images/ehh.jpg") match {
    case Some(im) => im
    case _ => Array[Byte]()
  }
  val image2Res = Response(Status.Ok,Headers(
    Header.ContentType(MediaType.image.jpeg)
  ) , Body.fromArray(image2))





  val routes = Routes(
    Method.GET / "test" ->  testRes.toHandler,
    Method.GET / Root ->  mainRes.toHandler,
    Method.GET / "Actual" -> handler{
      println(AppData.toJson.toString)
      Response.json(AppData.toJson)
    },
    Method.GET / "images" / "gol.jpg" -> image1Res.toHandler,
    Method.GET / "images" / "ehh.jpg" -> image2Res.toHandler,
    Method.POST / "fire" -> handler{ (req: Request)=>{

      val runtime = Runtime.default

      val result: String = Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(req.body.asString).getOrThrow()
      }

      val res = result.fromJson[ReqRes] match {
      case Right(rs) => rs
      case _ => {
        println("ERRRO json")
          ReqRes(0,0,1)
      }
    }
      val endTime = Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(Clock.nanoTime).getOrThrow()
      }
      val workDuration = Duration.ofNanos(endTime - startTime)
      val formattedOnTime = java.time.LocalDateTime.now.format(formatter)



        val resp = MyRecord(checkInside(res),res.X,res.Y, res.R,formattedOnTime,workDuration.toSeconds )
        AppData+= resp
        Response.json(resp.toJson.toString)

    }
}
  )



  def run = Server.serve(routes).provide(Server.defaultWithPort(8052))
}