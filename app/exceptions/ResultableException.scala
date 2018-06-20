package exceptions

import play.api.mvc.Result
import play.api.mvc.Results._
import scala.util.{Try, Failure}

trait ResultableException {
  val httpResult: Result
}

object ResultableException {
  def handle[T](successHandler: PartialFunction[Try[T], Result]): PartialFunction[Try[T], Result] = {
      successHandler orElse {
        case Failure(re: ResultableException) => re.httpResult
        case Failure(_: Throwable) => InternalServerError
      }
  }
}