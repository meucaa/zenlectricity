package models

import play.api.libs.json._
import com.typesafe.config.ConfigFactory

case class Url(path: String)

object Url {
  private val playHttpContext = ConfigFactory.load().getString("play.http.context");
  def apply(path: String): Url = new Url(playHttpContext + path)
  implicit def urlToJsString(url: Url): JsString = JsString(url.path)
}
