package controllers

import services.CryptoService

import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}

class AuthorizedAction[T](cryptoService: CryptoService, 
                       bparser: BodyParser[T])(implicit val executionContext: ExecutionContext) 
                       extends ActionBuilder[UserRequest, T] {

  override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {
    request.headers.get("Authorization")
      .flatMap(token => {
        cryptoService.decodeToken(token).map(user => block(new UserRequest(user, request)))
      })
      .getOrElse(Future(Results.Unauthorized))
  }
  override def parser = bparser
}

object AuthorizedAction {
  def apply[T](cryptoService: CryptoService, 
               parser: BodyParser[T])
              (implicit ec: ExecutionContext): AuthorizedAction[T] = new AuthorizedAction(cryptoService, parser)(ec)
}