package repositories

import models._
import commands._
import exceptions._

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.util.{Try, Success, Failure}
import scala.concurrent.{ Future, ExecutionContext }

@Singleton
class PowerStationRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class PowerStationTable(tag: Tag) extends Table[PowerStation](tag, "powerstation") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def `type` = column[String]("type")
    def capacity = column[Double]("capacity")
    def userId = column[Long]("user_id")

    def * = (id, `type`, capacity, userId) <> ((PowerStation.apply _).tupled, PowerStation.unapply)
  }

  private val powerStations = TableQuery[PowerStationTable]

  def create(userId: Long, createPwsCommand: CreatePowerStation): Future[PowerStation] = db.run {
    ((powerStations.map(pws => (pws.`type`, pws.capacity, pws.userId))
      returning powerStations.map(_.id)
      into ((pws, id) => PowerStation(id, pws._1, pws._2, pws._3))
    ) += (createPwsCommand.`type`, createPwsCommand.capacity, userId))
  }

  def list(userId: Long): Future[Seq[PowerStation]] = db.run {
    powerStations.filter(_.userId === userId).result
  }

  def listByType(userId: Long, `type`: String): Future[Seq[PowerStation]] = db.run {
    powerStations.filter(pws => pws.userId === userId && pws.`type` === `type`).result
  }
  
  def fetchById(userId: Long, powerStationId: Long): Future[Try[PowerStation]] = db.run {
    powerStations.filter(pws => pws.userId === userId && pws.id === powerStationId).result.headOption.map {
      case Some(pws) => Success(pws)
      case None      => Failure(PowerStationNotFound(powerStationId))
    }
  }

 
}