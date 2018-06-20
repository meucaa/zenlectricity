package models

import play.api.libs.json._

case class User(id: Long, login: String, password: String)

object User {
  implicit val userFormat = Json.format[User]
}

case class UserForm(login: String, password: String)

object UserForm {
  implicit val userFormFormat: Format[UserForm] = Json.format[UserForm]
}

case class UserInfos(id: Long, login: String)

object UserInfos {
  implicit val userInfosFormat: Format[UserInfos] = Json.format[UserInfos]
}
