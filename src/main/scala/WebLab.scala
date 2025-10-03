package mainSc

import zio.{http, _}
import zio.http.{codec, _}
import zio.http.codec.PathCodec
import zio.json._

import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.io.{BufferedSource, Source}
import java.io.{IOException, InputStream}
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import java.util.UUID

object MainApp extends ZIOAppDefault {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")


  val appData: Ref[Map[String, ArrayBuffer[MyRecord]]] = Unsafe.unsafe { implicit unsafe =>
    Runtime.default.unsafe.run(Ref.make(Map.empty[String, ArrayBuffer[MyRecord]])).getOrThrow()
  }

  def getOrCreateClientId(request: Request): (String, Option[Cookie.Response]) = {
    request.cookie("clientId") match {
      case Some(cookie) =>
        (cookie.content, None)
      case None =>
        val newId = UUID.randomUUID().toString
        val newCookie = Cookie.Response(
          name = "clientId",
          content = newId,
          path = Some(Path.root),
          maxAge = Some(java.time.Duration.ofDays(365))
        )
        (newId, Some(newCookie))
    }
  }


  def checkInside(req: ReqRes): Boolean = {
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

  def readResource(path: String): IO[IOException, String] = {
    ZIO.attempt {
      val classLoader = getClass.getClassLoader
      val inputStream: InputStream = classLoader.getResourceAsStream(path)
      if (inputStream == null) throw new IOException(s"Ресурс не найден: $path")

      try {
        val source: BufferedSource = Source.fromInputStream(inputStream, "UTF-8")
        try source.mkString finally source.close()
      } finally inputStream.close()
    }.refineToOrDie[IOException]
  }

  def readResourceByte(path: String): IO[IOException, Array[Byte]] = {
    ZIO.attempt {
      val classLoader = getClass.getClassLoader
      val inputStream: InputStream = classLoader.getResourceAsStream(path)
      if (inputStream == null) throw new IOException(s"Ресурс не найден: $path")

      try inputStream.readAllBytes()
      finally inputStream.close()
    }.refineToOrDie[IOException]
  }

  val mainPageContent: ZIO[Any, IOException, String] = readResource("static/index.html")

  val routes: Routes[Any, Response] = Routes(
    Method.GET / Root -> handler {
      mainPageContent.fold(
        _ => Response.text("404 - Главная страница не найдена").status(Status.NotFound),
        content => Response(Status.Ok, Headers.empty, Body.fromString(content))
      )
    },

    Method.GET / "clear" -> handler { (request: Request) =>
      val (clientId, _) = getOrCreateClientId(request)
      for {
        _ <- appData.update(currentMap => currentMap + (clientId -> ArrayBuffer.empty[MyRecord]))
        clientData <- appData.get.map(_.getOrElse(clientId, ArrayBuffer.empty))
      } yield Response.json(clientData.toJson)
    },

    Method.GET / "actual" -> handler { (request: Request) =>
      val (clientId, _) = getOrCreateClientId(request)
      appData.get.map { dataMap =>
        val clientHistory = dataMap.getOrElse(clientId, ArrayBuffer.empty[MyRecord])
        Response.json(clientHistory.toJson)
      }
    },

    Method.GET / "images" / string("imageName") -> handler { (imageName: String, _: Request) =>
      readResourceByte(s"images/$imageName").fold(
        _ => Response.status(Status.NotFound),
        bytes => Response(
          Status.Ok,
          Headers(Header.ContentType(MediaType.image.jpeg)),
          Body.fromArray(bytes)
        )
      )
    },

    Method.GET / "fire" -> handler { (request: Request) =>
      val (clientId, cookieOption) = getOrCreateClientId(request)

      val effect: ZIO[Any, Response, Response] = for {
        startTime <- Clock.nanoTime

        xVal <- ZIO.fromOption(request.queryParam("X").flatMap(s => Try(s.toInt).toOption))
          .mapError(_ => Response.text("неверный или отсутствующий параметр 'X'").status(Status.BadRequest))

        yVal <- ZIO.fromOption(request.queryParam("Y").flatMap(s => Try(s.toInt).toOption))
          .mapError(_ => Response.text("неверный или отсутствующий параметр 'Y'").status(Status.BadRequest))

        rVal <- ZIO.fromOption(request.queryParam("R").flatMap(s => Try(s.toInt).toOption))
          .mapError(_ => Response.text("неверный или отсутствующий параметр 'R'").status(Status.BadRequest))

        req = ReqRes(xVal, yVal, rVal)
        isInside = checkInside(req)
        endTime <- Clock.nanoTime
        workDuration = endTime - startTime
        formattedOnTime = LocalDateTime.now.format(formatter)
        resp = MyRecord(isInside, req.X, req.Y, req.R, formattedOnTime, workDuration)

        _ <- appData.update { currentMap =>
          val clientHistory = currentMap.getOrElse(clientId, ArrayBuffer.empty[MyRecord])
          clientHistory += resp
          currentMap + (clientId -> clientHistory)
        }

        baseResponse = Response.json(resp.toJson)

        finalResponse = cookieOption.map(baseResponse.addCookie).getOrElse(baseResponse)

      } yield finalResponse

      effect.merge
    }
  )

  def run = Server.serve(routes).provide(Server.defaultWithPort(8052))
}