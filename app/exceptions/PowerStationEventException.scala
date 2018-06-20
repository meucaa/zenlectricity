package exceptions

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.libs.json.Json

sealed trait PowerStationEventException extends RuntimeException with ResultableException

case class PowerStationEventNotFound(powerStationId: Long, eventId: Long) extends PowerStationEventException {
  val httpResult: Result = NotFound(Json.obj("error" -> s"Event ${eventId} of powerstation ${powerStationId} does not exist"))
}

case class AmountTooLarge(powerStationId: Long, currentBalance: Double, amount: Double, capacity: Double) extends PowerStationEventException {
  val httpResult: Result = BadRequest(
    Json.obj("error" -> s"Amount ${amount} cannot be added to powerstation ${powerStationId} (${currentBalance} / ${capacity})")
  )
}