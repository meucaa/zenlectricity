package models

import play.api.libs.json._
import com.typesafe.config.ConfigFactory

case class Resource[T](urlName: String, relativeUrlToResource: String, resource: T) {
  private val playHttpContext = ConfigFactory.load().getString("play.http.context");
  def toJson(implicit w: Writes[T]): JsValue = Json.obj(
                                                    urlName -> (playHttpContext + relativeUrlToResource),
                                                    "resource" -> Json.toJson(resource)
                                                  )
}
