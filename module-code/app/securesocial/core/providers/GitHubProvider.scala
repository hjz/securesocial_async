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

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{Response, WS}
import play.api.{Logger, Application}
import scala.concurrent._
import scala.util.{Success, Failure}
import securesocial.core._
import securesocial.core.providers.GitHubProvider._

/**
 * A GitHub provider
 *
 */
class GitHubProvider(application: Application) extends OAuth2Provider(application) {

  def providerId = GitHubProvider.GitHub

  override protected def buildInfo(response: Response): OAuth2Info = {
    response.body.split("&|=") match {
      case Array(AccessToken, token, TokenType, tokenType) => OAuth2Info(token, Some(tokenType), None)
      case _ =>
        Logger.error("Invalid response format for accessToken")
        throw new AuthenticationException()
    }
  }

  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param user The user object to be populated
   * @return A copy of the user object with the new values set
   */
  def fillProfile(user: SocialUser): Future[SocialUser] = {
    val call = WS.url(GetAuthenticatedUser.format(user.oAuth2Info.get.accessToken)).get()
    val socialUserPromise = promise[SocialUser]
    call.onComplete {
      case Failure(t) =>
        Logger.error("Error retrieving profile information from github", t)
        throw new AuthenticationException()
      case Success(response) =>
        val me = response.json
        (me \ Message).asOpt[String] match {
          case Some(msg) => {
            Logger.error("Error retrieving profile information from GitHub. Message = %s".format(msg))
            throw new AuthenticationException()
          }
          case _ =>
            socialUserPromise.success {
              val userId = (me \ UserId).as[Int]
              val displayName = (me \ Name).asOpt[String].getOrElse("")
              val avatarUrl = (me \ AvatarUrl).asOpt[String]
              val email = (me \ Email).asOpt[String].filter(!_.isEmpty)
              user.copy(
                id = Id(userId.toString, providerId),
                fullName = displayName,
                avatarUrl = avatarUrl,
                email = email
              )
            }
        }
    }
    socialUserPromise.future
  }

}

object GitHubProvider {
  val GitHub = "github"
  val GetAuthenticatedUser = "https://api.github.com/user?access_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Message = "message"
  val UserId = "id"
  val Name = "name"
  val AvatarUrl = "avatar_url"
  val Email = "email"
}