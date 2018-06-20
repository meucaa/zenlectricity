package exception

import org.scalacheck._

import exceptions._
import play.api.test.Helpers._


object Exception extends Properties("Exception") {
  import Prop.forAll

  property("UserSignupFailed") = forAll { (login: String) =>
    UserSignupFailed(login).httpResult.header.status == CONFLICT
  }

  property("UserAuthenticationFailed") = forAll { (login: String) =>
    UserAuthenticationFailed(login).httpResult.header.status == UNAUTHORIZED
  }

  property("PowerStationNotFound") = forAll { (id: Long) =>
    PowerStationNotFound(id).httpResult.header.status == NOT_FOUND
  }

  property("PowerStationEventNotFound") = forAll { (powerStationId: Long, eventId: Long) =>
    PowerStationEventNotFound(powerStationId, eventId)
                             .httpResult.header.status == NOT_FOUND
  }

  property("AmountTooLarge") = forAll { (powerStationId: Long, 
                                         currentBalance: Double, 
                                         amount: Double, 
                                         capacity: Double) =>
    AmountTooLarge(powerStationId, currentBalance, amount, capacity)
                  .httpResult.header.status == BAD_REQUEST
  }
}