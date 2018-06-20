package models

import play.api.libs.json._
import java.sql.Timestamp
import java.text.SimpleDateFormat

case class PowerStationEvent(id: Long, timestamp: Timestamp, amount: Double, powerStationId: Long)

object PowerStationEvent {
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
    def reads(json: JsValue) = JsSuccess(new Timestamp(format.parse(json.as[String]).getTime))
    def writes(ts: Timestamp) = JsNumber(ts.getTime / 1000)
  }

  implicit val powerStationFormat: Format[PowerStationEvent] = Json.format[PowerStationEvent]
}