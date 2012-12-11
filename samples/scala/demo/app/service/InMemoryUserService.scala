/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import play.api.libs.concurrent.Execution.Implicits._
import play.api.{Logger, Application}
import scala.concurrent.Future
import securesocial.core.providers.Token
import securesocial.core.{UserServicePlugin, UserId, SocialUser}


/**
 * A Sample In Memory user service in Scala
 *
 * IMPORTANT: This is just a sample and not suitable for a production environment since
 * it stores everything in memory.
 */
class InMemoryUserService(application: Application) extends UserServicePlugin(application) {
  private var users = Map[String, SocialUser]()
  private var tokens = Map[String, Token]()

  def find(id: UserId): Future[Option[SocialUser]] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("users = %s".format(users))
    }
    Future(users.get(id.id + id.providerId))
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[SocialUser]] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("users = %s".format(users))
    }
    Future(users.values.find( u => u.email.map( e => e == email && u.id.providerId == providerId).getOrElse(false)))
  }

  def save(user: SocialUser) {
    users = users + (user.id.id + user.id.providerId -> user)
  }

  def save(token: Token) {
    tokens += (token.uuid -> token)
  }

  def findToken(token: String): Future[Option[Token]] = {
    Future(tokens.get(token))
  }

  def deleteToken(uuid: String) {
    tokens -= uuid
  }

  def deleteTokens() {
    tokens = Map()
  }

  def deleteExpiredTokens() {
    tokens = tokens.filter(!_._2.isExpired)
  }
}
