package repositories

import services.CryptoService
import models._
import exceptions._

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ Future, ExecutionContext }
import scala.util.{Try, Success, Failure}

@Singleton
class UserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, crypto: CryptoService)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._
  import java.sql.SQLIntegrityConstraintViolationException

  private class UserTable(tag: Tag) extends Table[User](tag, "user") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def login = column[String]("login")
    def password = column[String]("password")

    def * = (id, login, password) <> ((User.apply _).tupled, User.unapply)
  }

  private val users = TableQuery[UserTable]

  private def filterByLogin(login: String) = users.filter(user => user.login === login).result.headOption

  private def insert(userForm: UserForm): Future[Try[User]] = db.run {
    ((users.map(u => (u.login, u.password))
          returning users.map(_.id)
          into ((loginPassword, id) => User(id, loginPassword._1, loginPassword._2))
        ) += (userForm.login, crypto.encryptPassword(userForm.password))).asTry
  }

  def create(userForm: UserForm): Future[Try[User]] =
    insert(userForm).map { tryUser =>
      tryUser.recoverWith {
        case _: SQLIntegrityConstraintViolationException => Failure(UserSignupFailed(userForm.login))
      }
    }

  def fetchByLogin(login: String) : Future[Try[User]] = db.run {
    filterByLogin(login).map {
      case Some(u: User) => Success(u)
      case None => Failure(UserAuthenticationFailed(login))
    }
  }
}
