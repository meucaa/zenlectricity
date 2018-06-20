package models

import org.scalacheck._

import test.Gens._

object PowerStationSpec extends Properties("PowerStation") {
  import Prop.forAll

  property("computeState") = forAll { (pws: PowerStation, events: List[PowerStationEvent]) =>
    pws.computeState(events).balance == events.map(_.amount).fold(0d)(_+_)
  }
}