package controllers

import models._
import models.UserForm._
import exceptions._
import repositories.UserRepository
import services.CryptoService

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.util.Success
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject()(userRepo: UserRepository, crypto: CryptoService, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def signup = Action.async(parse.json(userFormFormat)) { implicit request: Request[UserForm] =>
    val userForm: UserForm = request.body
    userRepo.create(userForm).map {
      ResultableException.handle {
        case Success(user) => {
          val signupResult = UserInfos(user.id, user.login)
          val createdResource = Resource("loginUrl", s"/login", signupResult)
          Created(createdResource.toJson)
        }
      }
    }
  }

  def login = Action.async(parse.json(userFormFormat)) { implicit request: Request[UserForm] =>
    val userForm: UserForm = request.body
    userRepo.fetchByLogin(userForm.login).map {
      ResultableException.handle {
        case Success(fetchedUser) => {
          val isPasswordOk = crypto.validatePassword(userForm.password, fetchedUser.password)
          if(isPasswordOk) {
            val authorization = AuthorizedLogin(fetchedUser.login, crypto.generateToken(fetchedUser))
            val authorizationJson = Json.toJson(authorization).as[JsObject] +
                                    ("powerStationsUrl" -> Url("/powerstations"))
            Ok(authorizationJson)
          }
          else Unauthorized
        }
      }
    }
  }
}
