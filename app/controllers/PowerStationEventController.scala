package controllers

import models._
import commands._
import commands.CreatePowerStationEvent._
import exceptions._
import services.CryptoService
import repositories.PowerStationEventRepository

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json._

import scala.util.Success
import scala.concurrent.ExecutionContext

@Singleton
class PowerStationEventController @Inject()(pseRepo: PowerStationEventRepository, 
                                            cryptoService: CryptoService, 
                                            cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private def create(userId: Long, powerStationId: Long, command: CreatePowerStationEvent) = {
    pseRepo.createEvent(userId, powerStationId, command).map {
      ResultableException.handle {
        case Success(event) => {
          val createdEvent = Resource("powerStationEventUrl",
                                      s"/powerstations/${event.powerStationId}/events/${event.id}",
                                      event)
          Created(createdEvent.toJson)
        }
      }
    }
  }

  def load(powerStationId: Long) = AuthorizedAction(cryptoService, parse.json(createPowerStationEventFormat)).async { request =>
    val pwseCommand: CreatePowerStationEvent = request.body
    val loadCommand = CreatePowerStationEvent(Math.abs(pwseCommand.amount))
    create(request.user.id, powerStationId, loadCommand)
  }

  def consume(powerStationId: Long) = AuthorizedAction(cryptoService, parse.json(createPowerStationEventFormat)).async { request =>
    val pwseCommand: CreatePowerStationEvent = request.body
    val consumeCommand = CreatePowerStationEvent(- Math.abs(pwseCommand.amount))
    create(request.user.id, powerStationId, consumeCommand)
  }

  def list(powerStationId: Long) = AuthorizedAction(cryptoService, parse.empty).async { request =>
    pseRepo.list(request.user.id, powerStationId).map {
      ResultableException.handle {
        case Success(pwsEvents) => {
          val locatedUserPwsEvents = pwsEvents
                                    .map(pws => Resource("powerStationEventUrl", 
                                                        s"/powerstations/${pws.powerStationId}/events/${pws.id}",
                                                        pws))
                                    .map(_.toJson)
          Ok(JsArray(locatedUserPwsEvents))
        }
      }
    }
  }

  def fetch(powerStationId: Long, eventId: Long) = AuthorizedAction(cryptoService, parse.empty).async { request =>
    pseRepo.fetch(request.user.id, powerStationId, eventId).map {
      ResultableException.handle {
        case Success(event) => Ok(Json.toJson(event))
      }
    }
  }

  def balance = AuthorizedAction(cryptoService, parse.empty).async { request =>
    pseRepo.balance(request.user.id).map(balance => {
      val balanceJson = Json.toJson(balance).as[JsObject] + ("powerStationsUrl" -> Url("/powerstations"))
      Ok(Json.toJson(balanceJson))
    })
  }
}
