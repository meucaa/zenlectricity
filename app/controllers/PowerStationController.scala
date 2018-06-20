package controllers

import models._
import commands._
import commands.CreatePowerStation._
import services.CryptoService
import repositories._
import exceptions._

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.util.Success
import scala.concurrent.ExecutionContext

@Singleton
class PowerStationController @Inject()(psRepo: PowerStationRepository,
                                       pseRepo: PowerStationEventRepository,
                                       cryptoService: CryptoService, 
                                       cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  def create = AuthorizedAction(cryptoService, parse.json(createPowerStationFormat)).async { request =>
    val createCommand: CreatePowerStation = request.body
    psRepo.create(request.user.id, createCommand).map(createdPws => {
      val createdPowerstation = Resource("powerStationUrl", s"/powerstations/${createdPws.id}", createdPws)
      Created(createdPowerstation.toJson)
    })
  }

  def list(`type`: Option[String]) = AuthorizedAction(cryptoService, parse.empty).async { request =>
    val fetching = `type` match {
                      case Some(t) => psRepo.listByType(request.user.id, t)
                      case None => psRepo.list(request.user.id)
                    }
    fetching.map(userPws => {
      val locatedUserPws = userPws.map(pws => Resource("powerStationUrl", s"/powerstations/${pws.id}", pws))
                                  .map(_.toJson)
      Ok(JsArray(locatedUserPws))
    })
  }

  def fetchById(powerStationId: Long) = AuthorizedAction(cryptoService, parse.empty).async { request =>
    pseRepo.fetchPowerStationWithEvents(request.user.id, powerStationId).map {
      ResultableException.handle {
        case Success(pws) => Ok(Json.toJson(pws))
      }
    }
  }
}
