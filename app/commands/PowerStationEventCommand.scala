package commands

import play.api.libs.json._

sealed trait PowerStationEventCommand

case class CreatePowerStationEvent(amount: Double) extends PowerStationEventCommand

object CreatePowerStationEvent {
  implicit val createPowerStationEventFormat: Format[CreatePowerStationEvent] = Json.format[CreatePowerStationEvent]
}
