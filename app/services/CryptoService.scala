package services

import com.google.inject.ImplementedBy
import models._

@ImplementedBy(classOf[AuthCryptoService])
trait CryptoService {
  def generateToken(user: User, validity: Option[Long] = None): String
  def decodeToken(token: String): Option[UserInfos]
  def encryptPassword(password: String): String
  def validatePassword(password: String, hashedPassword: String): Boolean
}
