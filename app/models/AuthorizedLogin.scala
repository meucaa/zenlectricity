package models

import play.api.libs.json._

case class AuthorizedLogin(login: String, token: String)

object AuthorizedLogin {
  implicit val authorizedLoginFormat: Format[AuthorizedLogin] = Json.format[AuthorizedLogin]
}