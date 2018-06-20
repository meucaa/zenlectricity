package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import repositories._
import services._
import models._
import commands._
import exceptions._
import play.api.libs.json._
import org.mockito.Mockito.{mock, when}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Failure}

class PowerStationControllerSpec extends PlaySpec with GuiceOneAppPerSuite {

  private val user = UserInfos(0, "zenlectricity")
  private val createCommand = CreatePowerStation("solar", 1000d)
  private val validToken = "validToken"
  private val badToken = "badToken"

  private val psRepo = mock(classOf[PowerStationRepository])
  private val pseRepo = mock(classOf[PowerStationEventRepository])
  private val crypto = mock(classOf[AuthCryptoService])
  private val pwController = new PowerStationController(psRepo, pseRepo, crypto, stubControllerComponents())


  private val pws = Seq(
                PowerStation(0, "solar", 1000d, user.id),
                PowerStation(1, "wind", 100d, user.id),
                PowerStation(2, "solar", 123d, user.id)
               )
  
  private val pwWithEvents = PowerStationWithEvents(pws.head.id, pws.head.`type`, pws.head.capacity, 0d, pws.head.userId, Seq())

  private def FakeGetRequest(url: String, token: String) = FakeRequest[Unit](GET, url, FakeHeaders(Seq(("Authorization", token))), Nil)

  when(crypto.decodeToken(validToken)).thenReturn(Some(user))
  when(crypto.decodeToken(badToken)).thenReturn(None)

  "list" should {
    val listUrl = "/api/powerstations"

    "return ok (200) with powerstations list" in {
      when(psRepo.list(user.id)).thenReturn(Future(pws))
      val request = FakeGetRequest(listUrl, validToken)
      val result: Future[Result] = pwController.list(None).apply(request)
      status(result) mustEqual OK
      val resultContent = contentAsJson(result).as[JsArray]
      resultContent.value.size mustEqual pws.length
      resultContent.value.map(pw => {
        val pwObject = pw.as[JsObject]
        pwObject.keys must contain allOf("powerStationUrl", "resource")
        (pwObject \ "resource").as[JsObject].keys must contain allOf("id", "type", "capacity", "userId")
      })
    }
    "return ok (200) with filtered by type powerstations list" in {
      val chosenType = "solar"
      when(psRepo.listByType(user.id, chosenType)).thenReturn(Future(pws.filter(_.`type`== chosenType)))
      val request = FakeGetRequest(listUrl, validToken)
      val result: Future[Result] = pwController.list(Some(chosenType)).apply(request)
      status(result) mustEqual OK
      val resultContent = contentAsJson(result).as[JsArray]
      resultContent.value.size mustEqual pws.filter(_.`type`== chosenType).length
      resultContent.value.map(pw => {
        val pwObject = pw.as[JsObject]
        pwObject.keys must contain allOf("powerStationUrl", "resource")
        (pwObject \ "resource").as[JsObject].keys must contain allOf("id", "type", "capacity", "userId")
        (pwObject \ "resource" \ "type").as[String] mustEqual chosenType
      })
    }
    "return ok (200) with empty powerstations list" in {
      when(psRepo.list(user.id)).thenReturn(Future(Seq()))
      val request = FakeGetRequest(listUrl, validToken)
      val result: Future[Result] = pwController.list(None).apply(request)
      status(result) mustEqual OK
      val resultContent = contentAsJson(result).as[JsArray]
      resultContent.value.size mustEqual 0
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeGetRequest(listUrl, badToken)
      val result: Future[Result] = pwController.list(None).apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }

  "create" should {
    val createUrl = "/api/powerstations"

    "return created (201) when powerstation is created" in {
      when(psRepo.create(user.id, createCommand)).thenReturn(Future(PowerStation(0, createCommand.`type`, createCommand.capacity, user.id)))
      val request = FakeRequest(POST, createUrl).withHeaders(("Authorization", validToken)).withBody(createCommand)
      val result: Future[Result] = pwController.create.apply(request)
      status(result) mustEqual CREATED
      contentAsJson(result).as[JsObject].keys must contain allOf("powerStationUrl", "resource")
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeRequest(POST, createUrl).withHeaders(("Authorization", badToken)).withBody(createCommand)
      val result: Future[Result] = pwController.create.apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }
  
  "fetchById" should {
    def fetchUrl(id: Long) = s"/api/powerstations/${id}"

    "return ok (200) with a powerstation" in {
      when(pseRepo.fetchPowerStationWithEvents(user.id, pwWithEvents.id)).thenReturn(Future(Success(pwWithEvents)))
      val request = FakeGetRequest(fetchUrl(pwWithEvents.id), validToken)
      val result: Future[Result] = pwController.fetchById(pwWithEvents.id).apply(request)
      status(result) mustEqual OK
      contentAsJson(result).as[JsObject].keys must contain allOf("id", "type", "capacity", "userId", "events")
    }
    "return not found (404) if powerstation id does not exist for user" in {
      when(pseRepo.fetchPowerStationWithEvents(user.id, 2311)).thenReturn(Future(Failure(PowerStationNotFound(2311))))
      val request = FakeGetRequest(fetchUrl(2311), validToken)
      val result: Future[Result] = pwController.fetchById(2311).apply(request)
      status(result) mustEqual NOT_FOUND
      contentAsJson(result).as[JsObject].keys must contain("error")
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeGetRequest(fetchUrl(pwWithEvents.id), badToken)
      val result: Future[Result] = pwController.fetchById(pwWithEvents.id).apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }
}
