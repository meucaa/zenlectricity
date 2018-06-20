package services

import models._

import javax.inject.Inject

import play.api.Configuration
import play.api.libs.json.Json

import pdi.jwt.{JwtJson, JwtAlgorithm, JwtClaim}
import com.github.t3hnar.bcrypt._

class AuthCryptoService @Inject()(configuration: Configuration) extends CryptoService {
  private val Algorithm = JwtAlgorithm.HS256
  private val JwtPlaySecret: String = configuration.get[String]("jwt.secret")
  private val BcryptSalt: String = configuration.get[String]("bcrypt.salt")
  private val ValidityDuration: Long = 60 * 60 // 1 hour

  def generateToken(user: User, validity: Option[Long] = None): String = {
    val userObject = Json.obj("id" -> user.id, "login" -> user.login)
    val claim = JwtClaim(Json.stringify(userObject)).issuedNow.expiresIn(validity.getOrElse(ValidityDuration))
    JwtJson.encode(claim, JwtPlaySecret, Algorithm)
  }
  def decodeToken(token: String): Option[UserInfos] = {
    val decodedToken = JwtJson.decodeJson(token, JwtPlaySecret, Seq(Algorithm)).toOption
    decodedToken.flatMap(json => json.validate[UserInfos].asOpt)
  }
  def encryptPassword(password: String): String = password.bcrypt(BcryptSalt)
  def validatePassword(userPassword: String, bcryptedPassword: String): Boolean = {
    userPassword.isBcrypted(bcryptedPassword)
  }
}
