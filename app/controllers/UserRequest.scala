package controllers

import play.api.mvc._
import models.UserInfos

class UserRequest[A](val user: UserInfos, val request: Request[A]) extends WrappedRequest[A](request)
