package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.test._
import play.api.test.Helpers._
import repositories._
import services._
import models._
import exceptions._
import play.api.libs.json.{JsObject, Json}
import org.mockito.Mockito.{mock, when}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Failure}

class UserControllerSpec extends PlaySpec with GuiceOneAppPerSuite {

  private val user = UserForm("zenlectricity", "password456")
  private val HttpVerbs = List(GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS)

  private val userRepo = mock(classOf[UserRepository])
  private val crypto = mock(classOf[AuthCryptoService])
  private val userController = new UserController(userRepo, crypto, stubControllerComponents())

  "signup" should {
    val signupUrl = "/api/signup"

    "return a conflict (409) if login is already taken" in {
      when(userRepo.create(user)).thenReturn(Future(Failure(UserSignupFailed(user.login))))
      val request = FakeRequest(POST , signupUrl).withBody(user)
      val result = userController.signup.apply(request)
      status(result) mustEqual CONFLICT
      contentAsJson(result).as[JsObject].keys must contain("error")
      
    }
    "return a created (201) if user was sucessfully created" in {
      when(userRepo.create(user)).thenReturn(Future(Success(User(0, user.login, user.password))))
      val request = FakeRequest(POST, signupUrl).withBody(user)
      val result = userController.signup.apply(request)
      status(result) mustEqual CREATED
      contentAsJson(result).as[JsObject].keys must contain allOf("loginUrl", "resource")
    }
    "return not found (404) if HTTP verb is different from POST" in {
      HttpVerbs.filter(_ != POST).map(verb => {
        val Some(result) = route(fakeApplication, FakeRequest(verb, signupUrl))
        status(result) mustEqual NOT_FOUND
      })
    }
    "return a bad request (400) if failed to parse JSON" in {
      val wrongJson = Json.obj("nigol" -> "lucas", "drowssap" -> "123456789")
      val Some(result) = route(fakeApplication, FakeRequest(POST, signupUrl).withJsonBody(wrongJson))
      status(result) mustEqual BAD_REQUEST
    }
  }
  "login" should {
    val loginUrl = "/api/login"

    "return unauthorized (401) if login does not exist" in {
      when(userRepo.fetchByLogin(user.login)).thenReturn(Future(Failure(UserAuthenticationFailed(user.login))))
      val request = FakeRequest(POST, loginUrl).withBody(user)
      val result = userController.login.apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
    "return unauthorized (401) if incorrect password" in {
      when(userRepo.fetchByLogin(user.login)).thenReturn(Future(Success(User(0, user.login, user.password))))
      when(crypto.validatePassword(user.password, user.password)).thenReturn(false)
      val request = FakeRequest(POST, loginUrl).withBody(user)
      val result = userController.login.apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
    "return ok (200) with login and token if login match password" in {
      when(userRepo.fetchByLogin(user.login)).thenReturn(Future(Success(User(0, user.login, user.password))))
      when(crypto.validatePassword(user.password, user.password)).thenReturn(true)
      val request = FakeRequest(POST, loginUrl).withBody(user)
      val result = userController.login.apply(request)
      status(result) mustEqual OK
      contentAsJson(result).as[JsObject].keys must contain allOf("login", "token", "powerStationsUrl")
      (contentAsJson(result) \ "login").as[String] mustEqual user.login
    }
    "return not found (404) if HTTP verb is different from POST" in {
      HttpVerbs.filter(_ != POST).map(verb => {
        val Some(result) = route(fakeApplication, FakeRequest(verb, loginUrl))
        status(result) mustEqual NOT_FOUND
      })
    }
    "return a bad request (400) if failed to parse JSON" in {
      val wrongJson = Json.obj("nigol" -> "lucas", "drowssap" -> "123456789")
      val Some(result) = route(fakeApplication, FakeRequest(POST, loginUrl).withJsonBody(wrongJson))
      status(result) mustEqual BAD_REQUEST
    }
  }
}
