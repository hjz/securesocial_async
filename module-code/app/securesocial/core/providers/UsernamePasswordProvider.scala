/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core.providers

import com.typesafe.plugin._
import org.joda.time.DateTime
import play.api._
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import scala.concurrent._
import securesocial.controllers.TemplatesPlugin
import securesocial.core._
import utils.PasswordHasher
import securesocial.core.UserId
import securesocial.core.SocialUser
import scala.Some

/**
 * A username password provider
 */
class UsernamePasswordProvider(application: Application) extends IdentityProvider(application) {

  def providerId = UsernamePasswordProvider.UsernamePassword

  def authMethod = AuthenticationMethod.UserPassword

  val InvalidCredentials = "securesocial.login.invalidCredentials"

  def doAuth[A]()(implicit request: Request[A]): Future[Either[Result, SocialUser]] = {
    val form = UsernamePasswordProvider.loginForm.bindFromRequest()
    form.fold(
      errors => Future(Left(badRequest(errors, request))),
      credentials => {
        val userId = UserId(credentials._1, providerId)
        UserService.find(userId).map {
          case Some(user) if user.passwordInfo.isDefined &&
            use[PasswordHasher].matches(user.passwordInfo.get, credentials._2) =>
            Right(user)
          case _ =>
            Left(badRequest(UsernamePasswordProvider.loginForm, request, Some(InvalidCredentials)))
        }
      }
    )
  }

  private def badRequest[A](f: Form[(String, String)], request: Request[A], msg: Option[String] = None): PlainResult = {
    Results.BadRequest(use[TemplatesPlugin].getLoginPage(request, f, msg))
  }

  def fillProfile(user: SocialUser): Future[SocialUser] = {
    // nothing to do for this provider, the user should already have everything because it
    // was loaded from the backing store
    Future(user)
  }
}

object UsernamePasswordProvider {
  val UsernamePassword = "userpass"
  private val key = "securesocial.userpass.withUserNameSupport"
  private val sendWelcomeEmailKey = "securesocial.userpass.sendWelcomeEmail"
  private val enableGravatarKey = "securesocial.userpass.enableGravatarSupport"
  private val Hasher = "securesocial.userpass.hasher"
  private val EnableTokenJob = "securesocial.userpass.enableTokenJob"

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  val withUserNameSupport = current.configuration.getBoolean(key).getOrElse(false)
  val sendWelcomeEmail = current.configuration.getBoolean(sendWelcomeEmailKey).getOrElse(true)
  val enableGravatar = current.configuration.getBoolean(enableGravatarKey).getOrElse(true)
  val hasher = current.configuration.getString(Hasher).getOrElse(PasswordHasher.BCryptHasher)
  val enableTokenJob = current.configuration.getBoolean(EnableTokenJob).getOrElse(true)
}

/**
 * A token used for reset password and sign up operations
 *
 * @param uuid the token id
 * @param email the user email
 * @param creationTime the creation time
 * @param expirationTime the expiration time
 * @param isSignUp a boolean indicating wether the token was created for a sign up action or not
 */
case class Token(uuid: String, email: String, creationTime: DateTime, expirationTime: DateTime, isSignUp: Boolean) {
  def isExpired = expirationTime.isBeforeNow
}
