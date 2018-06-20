package commands

import play.api.libs.json._

sealed trait PowerStationCommand

case class CreatePowerStation(`type`: String, capacity: Double) extends PowerStationCommand

object CreatePowerStation {
  implicit val createPowerStationFormat: Format[CreatePowerStation] = Json.format[CreatePowerStation]
}