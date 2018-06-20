package services

import models._
import test.Gens._
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import org.scalacheck._
import org.scalacheck.Prop._
import org.scalacheck.Test.Parameters
import org.scalatest.prop.Checkers._


class AuthCryptoServiceSpec extends PlaySpec with BeforeAndAfter {

  private val config = mock(classOf[Configuration])

  implicit private val params = Parameters.default.withMinSuccessfulTests(200)
  private val longTestParams: Parameters = Parameters.default.withMinSuccessfulTests(5)

  private def checkIt(prop: Prop)(implicit p: Parameters) = check(prop, p)

  before({
    when(config.get[String]("jwt.secret")).thenReturn("verysecretstring")
    when(config.get[String]("bcrypt.salt")).thenReturn("$2a$10$S2st24nGvUMAJyn5sFc10e")
  })

  def acs = new AuthCryptoService(config)

  "generateToken" must {
    "be different for two different users" in {
      checkIt(forAll(pairDifferentUser)(p => {
        acs.generateToken(p._1) != acs.generateToken(p._2)
      }))
    }
    "depend on the timestamp" in {
      checkIt(forAll((u: User) => {
        val first = acs.generateToken(u)
        Thread.sleep(1000)
        val second = acs.generateToken(u)
        first != second
      }))(longTestParams)
    }
  }
  "decodeToken" must {
    "return None if token has expired" in {
      checkIt(forAll((u: User) => {
        acs.decodeToken(acs.generateToken(u, Some(-1))).isEmpty
      }))
    }
    "return None for random String" in {
      checkIt(forAll((str: String) => {
        acs.decodeToken(str).isEmpty
      }))
    }
    "return User is token is valid" in {
      checkIt(forAll((u: User) => {
        val user = acs.decodeToken(acs.generateToken(u))
        user.isDefined && user.get == UserInfos(u.id, u.login)
      }))
    }
  }
  "password encryption" must {
    "encrypt to a 60-length string" in {
      checkIt(forAll((pw: String) => {
        acs.encryptPassword(pw).length == 60
      }))(longTestParams)
    }
    "validate a previously encrypted password" in {
      checkIt(forAll((pw: String) => {
        acs.validatePassword(pw, acs.encryptPassword(pw))
      }))(longTestParams)
    }
  }
}
