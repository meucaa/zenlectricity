package repositories

import models._
import exceptions._
import commands._

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import java.sql.Timestamp

import scala.util.{Try, Success, Failure}
import scala.concurrent.{ Future, ExecutionContext }

@Singleton
class PowerStationEventRepository @Inject() (dbConfigProvider: DatabaseConfigProvider,
                                             psRepo: PowerStationRepository)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class PowerStationEventTable(tag: Tag) extends Table[PowerStationEvent](tag, "powerstation_event") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def timestamp = column[Timestamp]("ts")
    def amount = column[Double]("amount")
    def powerStationId = column[Long]("powerstation_id")

    def * = (id, timestamp, amount, powerStationId) <> ((PowerStationEvent.apply _).tupled, PowerStationEvent.unapply)
  }

  private val powerStationEvents = TableQuery[PowerStationEventTable]

  private def create(powerStationId: Long, createEventCommand: CreatePowerStationEvent) = db.run {
    ((powerStationEvents.map(pwsEvent => (pwsEvent.timestamp, pwsEvent.amount, pwsEvent.powerStationId))
      returning powerStationEvents.map(_.id)
      into ((pwsEvent, id) => PowerStationEvent(id, pwsEvent._1, pwsEvent._2, pwsEvent._3))
    ) += (new Timestamp(System.currentTimeMillis), createEventCommand.amount, powerStationId)).asTry
  }

  private def listAll: Future[Seq[PowerStationEvent]] = db.run { powerStationEvents.result }    

  private def listAllForUser(userId: Long): Future[Seq[PowerStationEvent]] = {
    (psRepo.list(userId) zip listAll).map {
      case (pws, events) => {
        val pwIds = pws.map(_.id)
        events.filter(e => pwIds.contains(e.powerStationId))
      }
    }
  }

  def createEvent(userId: Long, powerStationId: Long, createEventCommand: CreatePowerStationEvent): Future[Try[PowerStationEvent]] = {
    (psRepo.fetchById(userId, powerStationId) zip list(userId, powerStationId)).flatMap {
      case (Success(pw), Success(events)) => {
        val newEvent = PowerStationEvent(0, new Timestamp(0L), createEventCommand.amount, 0)
        val currentState = pw.computeState(events.toList)
        val newState = currentState.computeState(List(newEvent))
        if(newState.balance >= 0 && newState.balance <= pw.capacity) create(powerStationId, createEventCommand)
        else Future(Failure(AmountTooLarge(pw.id, currentState.balance, newEvent.amount, pw.capacity)))
      }
      case _ => Future(Failure(PowerStationNotFound(powerStationId)))
    }
  }
  
  def list(userId: Long, powerStationId: Long): Future[Try[Seq[PowerStationEvent]]] = {
    val fetchedPwsEvents = db.run { powerStationEvents.filter(_.powerStationId === powerStationId).result }
    (psRepo.fetchById(userId, powerStationId) zip fetchedPwsEvents).flatMap {
      case (Success(_), events) => Future(Success(events))
      case (Failure(_), _) => Future(Failure(PowerStationNotFound(powerStationId)))
    }
  }

  def fetch(userId: Long, powerStationId: Long, eventId: Long): Future[Try[PowerStationEvent]] = {
    val fetchedEvent = db.run { powerStationEvents
                                  .filter(e => e.powerStationId === powerStationId && e.id === eventId)
                                  .result.headOption }
    (psRepo.fetchById(userId, powerStationId) zip fetchedEvent).map {
      case (Success(_), Some(event)) => Success(event)
      case (Success(_), None) => Failure(PowerStationEventNotFound(powerStationId, eventId))
      case (Failure(_), _) => Failure(PowerStationNotFound(powerStationId))
    }
  }

  def fetchPowerStationWithEvents(userId: Long, powerStationId: Long): Future[Try[PowerStationWithEvents]] = {
    (psRepo.fetchById(userId, powerStationId) zip list(userId, powerStationId)).map {
      case (Success(pws), Success(events)) => {
        val currentBalance = pws.computeState(events.toList).balance
        Success(PowerStationWithEvents(pws.id, pws.`type`, pws.capacity, 
                                       currentBalance, pws.userId, events))
      } 
      case _ => Failure(PowerStationNotFound(powerStationId))
    }
  }

  def balance(userId: Long): Future[PowerStationBalance] = {
    listAllForUser(userId).map(events => {
      val loadedAmount   = events.map(_.amount).filter(_ >= 0).fold(0d)(_+_)
      val consumedAmount = events.map(_.amount).filter(_ < 0).fold(0d)(_+_)
      PowerStationBalance(userId, consumedAmount, loadedAmount)
    })
  }
}
