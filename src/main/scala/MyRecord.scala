package mainSc

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class MyRecord(isHit: Boolean ,X: Int, Y:Float,R:Int,OnTime:String ,WorkTime:Long )

object MyRecord {
  implicit val decoder: JsonDecoder[MyRecord] = DeriveJsonDecoder.gen[MyRecord]
  implicit val encoder: JsonEncoder[MyRecord] =
    DeriveJsonEncoder.gen[MyRecord]
}