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
import java.sql.Timestamp

class PowerStationEventControllerSpec extends PlaySpec with GuiceOneAppPerSuite {

  private val user = UserInfos(0, "zenlectricity")
  private val pw = PowerStation(1, "solar", 1000d, user.id)
  private val events = Seq(
                        PowerStationEvent(0, new Timestamp(1L), 100d, pw.id),
                        PowerStationEvent(0, new Timestamp(2L), -100d, pw.id),
                        PowerStationEvent(0, new Timestamp(3L), 1000d, pw.id)
                      )
  private val event = events.head
  private val validToken = "validToken"
  private val badToken = "badToken"
  private val loadCommand = CreatePowerStationEvent(100d)
  private val consumeCommand = CreatePowerStationEvent(-100d)

  private val pseRepo = mock(classOf[PowerStationEventRepository])
  private val crypto = mock(classOf[AuthCryptoService])
  private val pweController = new PowerStationEventController(pseRepo, crypto, stubControllerComponents())

  private def FakeGetRequest(url: String, token: String) = FakeRequest[Unit](GET, url, FakeHeaders(Seq(("Authorization", token))), Nil)

  when(crypto.decodeToken(validToken)).thenReturn(Some(user))
  when(crypto.decodeToken(badToken)).thenReturn(None)

  "list" should {
    val listUrl = "/api/powerstations/1/events"

    s"return ok (200) with powerstation ${pw.id} event list" in {
      when(pseRepo.list(user.id, pw.id)).thenReturn(Future(Success(events)))
      val request = FakeGetRequest(listUrl, validToken)
      val result: Future[Result] = pweController.list(pw.id).apply(request)
      status(result) mustEqual OK
      val resultContent = contentAsJson(result).as[JsArray]
      resultContent.value.size mustEqual events.length
      resultContent.value.map(event => {
        val eventObject = event.as[JsObject]
        eventObject.keys must contain allOf("powerStationEventUrl", "resource")
        (eventObject \ "resource").as[JsObject].keys must contain allOf("id", "timestamp", "amount", "powerStationId")
      })
    }
    s"return ok (200) with powerstation ${pw.id} empty event list" in {
      when(pseRepo.list(user.id, pw.id)).thenReturn(Future(Success(Seq())))
      val request = FakeGetRequest(listUrl, validToken)
      val result: Future[Result] = pweController.list(pw.id).apply(request)
      status(result) mustEqual OK
      contentAsJson(result).as[JsArray].value.size mustEqual 0
    }
    s"return not found (404) if powerstation does not exist" in {
      when(pseRepo.list(user.id, pw.id)).thenReturn(Future(Failure(PowerStationNotFound(pw.id))))
      val request = FakeGetRequest(listUrl, validToken)
      val result: Future[Result] = pweController.list(pw.id).apply(request)
      status(result) mustEqual NOT_FOUND
      contentAsJson(result).as[JsObject].keys must contain("error")
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeGetRequest(listUrl, badToken)
      val result: Future[Result] = pweController.list(pw.id).apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }

  "fetch" should {
    def fetchUrl(pwsId: Long, eventId: Long) = s"/api/powerstations/${pwsId}/events/${eventId}"

    s"return ok (200) with powerstation ${pw.id} event ${event.id}" in {
      when(pseRepo.fetch(user.id, pw.id, event.id)).thenReturn(Future(Success(event)))
      val request = FakeGetRequest(fetchUrl(pw.id, event.id), validToken)
      val result: Future[Result] = pweController.fetch(pw.id, event.id).apply(request)
      status(result) mustEqual OK
      val resultContent = contentAsJson(result).as[JsObject]
      resultContent.keys must contain allOf("id", "timestamp", "amount", "powerStationId")
    }
    "return not found (404) if powerstation does not exist" in {
      when(pseRepo.fetch(user.id, pw.id, event.id)).thenReturn(Future(Failure(PowerStationNotFound(pw.id))))
      val request = FakeGetRequest(fetchUrl(pw.id, event.id), validToken)
      val result: Future[Result] = pweController.fetch(pw.id, event.id).apply(request)
      status(result) mustEqual NOT_FOUND
      val resultContent = contentAsJson(result).as[JsObject]
      resultContent.keys must contain("error")
    }
    "return not found (404) if event does not exist" in {
      when(pseRepo.fetch(user.id, pw.id, event.id)).thenReturn(Future(Failure(PowerStationEventNotFound(pw.id, event.id))))
      val request = FakeGetRequest(fetchUrl(pw.id, event.id), validToken)
      val result: Future[Result] = pweController.fetch(pw.id, event.id).apply(request)
      status(result) mustEqual NOT_FOUND
      val resultContent = contentAsJson(result).as[JsObject]
      resultContent.keys must contain("error")
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeGetRequest(fetchUrl(pw.id, event.id), badToken)
      val result: Future[Result] = pweController.fetch(pw.id, event.id).apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }

  "load" should {
    def loadUrl(pwsId: Long) = s"/api/powerstations/${pwsId}/load"

    "return created (201) with created event" in {
      when(pseRepo.createEvent(user.id, pw.id, loadCommand))
        .thenReturn(Future(Success(PowerStationEvent(0, new Timestamp(1L), loadCommand.amount, pw.id))))
      val request = FakeRequest(POST, loadUrl(pw.id)).withHeaders(("Authorization", validToken)).withBody(loadCommand)
      val result: Future[Result] = pweController.load(pw.id).apply(request)
      status(result) mustEqual CREATED
      val resultContent = contentAsJson(result).as[JsObject]
      resultContent.keys must contain allOf("powerStationEventUrl", "resource")
      (resultContent \ "resource").as[JsObject].keys must contain allOf("id", "timestamp", "amount", "powerStationId")
    }
    "return bad request (400) if amount was exceeding capacity" in {
      when(pseRepo.createEvent(user.id, pw.id, loadCommand))
        .thenReturn(Future(Failure(AmountTooLarge(pw.id, 1000d, 1d, pw.capacity))))
      val request = FakeRequest(POST, loadUrl(pw.id)).withHeaders(("Authorization", validToken)).withBody(loadCommand)
      val result: Future[Result] = pweController.load(pw.id).apply(request)
      status(result) mustEqual BAD_REQUEST
      contentAsJson(result).as[JsObject].keys must contain("error")
    }
    "return not found (404) if powerstation does not exist" in {
      when(pseRepo.createEvent(user.id, pw.id, loadCommand))
        .thenReturn(Future(Failure(PowerStationNotFound(pw.id))))
      val request = FakeRequest(POST, loadUrl(pw.id)).withHeaders(("Authorization", validToken)).withBody(loadCommand)
      val result: Future[Result] = pweController.load(pw.id).apply(request)
      status(result) mustEqual NOT_FOUND
      contentAsJson(result).as[JsObject].keys must contain("error")
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeRequest(POST, loadUrl(pw.id)).withHeaders(("Authorization", badToken)).withBody(loadCommand)
      val result: Future[Result] = pweController.load(pw.id).apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }

  "consume" should {
    def consumeUrl(pwsId: Long) = s"/api/powerstations/${pwsId}/consume"

    "return created (201) with created event" in {
      when(pseRepo.createEvent(user.id, pw.id, consumeCommand))
        .thenReturn(Future(Success(PowerStationEvent(0, new Timestamp(1L), consumeCommand.amount, pw.id))))
      val request = FakeRequest(POST, consumeUrl(pw.id)).withHeaders(("Authorization", validToken)).withBody(consumeCommand)
      val result: Future[Result] = pweController.consume(pw.id).apply(request)
      status(result) mustEqual CREATED
      val resultContent = contentAsJson(result).as[JsObject]
      resultContent.keys must contain allOf("powerStationEventUrl", "resource")
      (resultContent \ "resource").as[JsObject].keys must contain allOf("id", "timestamp", "amount", "powerStationId")
    }
    "return bad request (400) if amount was exceeding capacity" in {
      when(pseRepo.createEvent(user.id, pw.id, consumeCommand))
        .thenReturn(Future(Failure(AmountTooLarge(pw.id, 1000d, -1d, pw.capacity))))
      val request = FakeRequest(POST, consumeUrl(pw.id)).withHeaders(("Authorization", validToken)).withBody(consumeCommand)
      val result: Future[Result] = pweController.consume(pw.id).apply(request)
      status(result) mustEqual BAD_REQUEST
      contentAsJson(result).as[JsObject].keys must contain("error")
    }
    "return not found (404) if powerstation does not exist" in {
      when(pseRepo.createEvent(user.id, pw.id, consumeCommand))
        .thenReturn(Future(Failure(PowerStationNotFound(pw.id))))
      val request = FakeRequest(POST, consumeUrl(pw.id)).withHeaders(("Authorization", validToken)).withBody(consumeCommand)
      val result: Future[Result] = pweController.consume(pw.id).apply(request)
      status(result) mustEqual NOT_FOUND
      contentAsJson(result).as[JsObject].keys must contain("error")
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeRequest(POST, consumeUrl(pw.id)).withHeaders(("Authorization", badToken)).withBody(consumeCommand)
      val result: Future[Result] = pweController.consume(pw.id).apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }

  "balance" should {
    val balanceUrl = "/api/powerstations/balance"
    "return load/consume balance for every powerstations" in {
      when(pseRepo.balance(user.id)).thenReturn(Future(PowerStationBalance(user.id, -100d, 100d)))
      val request = FakeGetRequest(balanceUrl, validToken)
      val result: Future[Result] = pweController.balance.apply(request)
      status(result) mustEqual OK
      val resultContent = contentAsJson(result).as[JsObject]
      resultContent.keys must contain allOf("userId", "consumed", "loaded", "powerStationsUrl")
      (resultContent \ "loaded").as[Double] >= 0 mustBe true
      (resultContent \ "consumed").as[Double] <= 0 mustBe true
    }
    "return unauthorized (401) if bad token" in {
      val request = FakeGetRequest(balanceUrl, badToken)
      val result: Future[Result] = pweController.balance.apply(request)
      status(result) mustEqual UNAUTHORIZED
    }
  }
}