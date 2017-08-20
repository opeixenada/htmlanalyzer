import javax.inject.Singleton

import play.api.http.HttpErrorHandler
import play.api.mvc.Results.{Status, _}
import play.api.mvc._

import scala.concurrent._


@Singleton
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(s"%s %s".format(statusCode,
        if (statusCode == 404) {
          "Page not found"
        } else {
          "A client error occurred: " + message
        }))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}
