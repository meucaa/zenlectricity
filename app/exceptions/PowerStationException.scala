package exceptions

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.libs.json.Json

sealed trait PowerStationException extends RuntimeException with ResultableException

case class PowerStationNotFound(id: Long) extends PowerStationException {
  val httpResult: Result = NotFound(Json.obj("error" -> s"Powerstation ${id} does not exist"))
}
