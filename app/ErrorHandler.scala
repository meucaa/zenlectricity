import javax.inject.Singleton

import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results._

import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {
  override def onClientError(request: RequestHeader, 
                             statusCode: Int,
                             message: String): Future[Result] = {
    Future.successful(Status(statusCode))
  }

  override def onServerError(request: RequestHeader, 
                             exception: Throwable): Future[Result] = {
    val errorJson = Json.obj("error" -> "Internal server error occured")
    Future.successful(InternalServerError(errorJson))
  }
}
