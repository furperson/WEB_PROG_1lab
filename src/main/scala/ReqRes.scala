package mainSc

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ReqRes(X: Int, Y:Float,R:Int)

object ReqRes {
  implicit val decoder: JsonDecoder[ReqRes] = DeriveJsonDecoder.gen[ReqRes]
  implicit val encoder: JsonEncoder[ReqRes] =
    DeriveJsonEncoder.gen[ReqRes]
}
