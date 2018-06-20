package exceptions

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.libs.json.Json

sealed trait UserException extends RuntimeException with ResultableException

case class UserSignupFailed(login: String) extends UserException {
  val httpResult: Result = Conflict(Json.obj("error" -> s"User ${login} already exists"))
}

case class UserAuthenticationFailed(login: String) extends UserException {
  val httpResult: Result = Unauthorized(Json.obj("error" -> s"User ${login} does not exist"))
}