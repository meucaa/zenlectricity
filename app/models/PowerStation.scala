package models

import play.api.libs.json._

case class PowerStation(id: Long, `type`: String, capacity: Double, userId: Long)

object PowerStation {
  implicit val powerStationFormat: Format[PowerStation] = Json.format[PowerStation]
  implicit def pwsToPwsState(pws: PowerStation) = PowerStationState(0d)
}

case class PowerStationState(balance: Double) {
  def computeState(events: List[PowerStationEvent]): PowerStationState = {
    events match {
      case Nil => this
      case h :: t => PowerStationState(balance + h.amount).computeState(t) 
    }
  }
}

case class PowerStationWithEvents(id: Long, `type`: String, capacity: Double, currentBalance: Double, userId: Long, events: Seq[PowerStationEvent])

object PowerStationWithEvents {
  implicit val powerStationWithEventsFormat: Format[PowerStationWithEvents] = Json.format[PowerStationWithEvents]
}

case class PowerStationBalance(userId: Long, consumed: Double, loaded: Double)

object PowerStationBalance {
  implicit val powerStationBalanceFormat: Format[PowerStationBalance] = Json.format[PowerStationBalance]
}
