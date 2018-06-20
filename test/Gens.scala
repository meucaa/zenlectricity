package test

import models._
import org.scalacheck.{Arbitrary, Gen}
import java.sql.Timestamp

object Gens {
  private val MaxId: Long = 100000L
  private val TimestampRangeSize = 10000L
  private val MaxAmount = 10000d
  private val MaxCapacity = 100000d
  val id: Gen[Long] = Gen.choose(1, MaxId)
  val timestamp: Gen[Timestamp] = Gen.choose(-TimestampRangeSize, TimestampRangeSize)
                                     .map(n => new Timestamp(System.currentTimeMillis + n))
  val amount: Gen[Double] = Gen.choose(-MaxAmount, MaxAmount)
  val capacity: Gen[Double] = Gen.choose(0, MaxCapacity)
  val user: Gen[User] = Gen.zip(id, Gen.alphaNumStr, Gen.alphaNumStr).map((User.apply _).tupled)
  val pairDifferentUser: Gen[(User, User)] = Gen.zip(user, user).suchThat(p => p._1.id != p._2.id && p._1.login != p._2.login)
  val powerStation: Gen[PowerStation] = Gen.zip(id, Gen.alphaNumStr, capacity, id)
                                           .map(x => PowerStation(x._1, x._2, x._3, x._4))
  val powerStationEvent: Gen[PowerStationEvent] = Gen.zip(id, timestamp, amount, id)
                                                     .map((PowerStationEvent.apply _).tupled)

  implicit val arbitraryUser: Arbitrary[User] = Arbitrary(user)
  implicit val arbitraryPowerStation: Arbitrary[PowerStation] = Arbitrary(powerStation)
  implicit val arbitraryPowerStationEvent: Arbitrary[PowerStationEvent] = Arbitrary(powerStationEvent)
}

