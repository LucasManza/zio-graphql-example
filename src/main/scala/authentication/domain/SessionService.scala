package authentication.domain

import authentication.domain.models.AuthDomain.{AuthenticatedSession, NonAuthenticatedSession, Session}
import zio.{FiberRef, IO, Task, ULayer, ZIO, ZLayer}

trait SessionService {
  def setSession(updateSession: Session): Task[Unit]

  def getAuthenticatedSession: Task[Option[AuthenticatedSession]]
}

object SessionServiceLive {
  val layer: ULayer[SessionService] = ZLayer.scoped {
    FiberRef
      .make[Session](NonAuthenticatedSession())
      .map { ref =>
        new SessionService {
          override def setSession(updateSession: Session): Task[Unit] = {
            ref.set(updateSession)
          }

          override def getAuthenticatedSession: Task[Option[AuthenticatedSession]] =
            ref.get.map {
              case NonAuthenticatedSession() => None
              case AuthenticatedSession(accountId) => Some(AuthenticatedSession(accountId))
            }
        }
      }
  }

  //  val unauthenticatedLayer: ULayer[AuthenticationSessionService] = ZLayer.succeed(new AuthenticationSessionService {
  //    override def getCurrentUser: IO[AuthenticationServiceError, AuthenticatedSession] =
  //      ZIO.fail(AuthenticationServiceError.AuthenticationRequired)
  //  })
  //
  //  def authenticatedLayer(authSession: AuthenticatedSession): ULayer[AuthenticationSessionService] = ZLayer.succeed(new AuthenticationSessionService {
  //    override def getCurrentUser: IO[AuthenticationServiceError, AuthenticatedSession] = ZIO.succeed(authSession)
  //  })
}