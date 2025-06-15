package authentication.domain

import authentication.domain.models.AuthDomain.AuthenticatedSession
import zio.{IO, ULayer, ZIO, ZLayer}

trait AuthenticationSessionService {
  def getAuthSession: IO[AuthenticationServiceError, AuthenticatedSession]
}


//object AuthSessionServiceLive {
//  val unauthenticatedLayer: ULayer[AuthenticationSessionService] = ZLayer.succeed(new AuthenticationSessionService {
//    override def getCurrentUser: IO[AuthenticationServiceError, AuthenticatedSession] =
//      ZIO.fail(AuthenticationServiceError.AuthenticationRequired)
//  })
//
//  def authenticatedLayer(authSession: AuthenticatedSession): ULayer[AuthenticationSessionService] = ZLayer.succeed(new AuthenticationSessionService {
//    override def getCurrentUser: IO[AuthenticationServiceError, AuthenticatedSession] = ZIO.succeed(authSession)
//  })
//}