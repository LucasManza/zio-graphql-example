package authentication

import authentication.domain.SessionService
import authentication.domain.models.AuthDomain.{AuthenticatedSession, NonAuthenticatedSession, Session}
import utils.JwtUtil
import zio.*
import zio.http.*


object CustomMiddleware {
  val authMiddleware = Middleware.customAuthZIO[SessionService] { (request: Request) =>
    println(s"Request: ${request}")
    request.headers.get(Header.Authorization) match {
      case Some(Header.Authorization.Bearer(token)) =>
        JwtUtil.decode(token.value.mkString) match {
          case Left(error) =>
            ZIO.serviceWithZIO[SessionService](_.setSession(NonAuthenticatedSession()))
              .orElseFail(Response.error(Status.InternalServerError))
              .as(true)
          case Right(accountId) =>
            ZIO.serviceWithZIO[SessionService](_.setSession(AuthenticatedSession(accountId)))
              .orElseFail(Response.error(Status.InternalServerError))
              .as(true)
        }
      case _ => ZIO.succeed(true)
    }
  }

  val middleware2: HandlerAspect[Any, Session] = Middleware.customAuthProviding[Session] { (request: Request) =>
    request.headers.get(Header.Authorization) match {
      case Some(Header.Authorization.Bearer(token)) =>
        JwtUtil.decode(token.value.mkString) match {
          case Left(error) =>
            Some(NonAuthenticatedSession())
          case Right(accountId) =>
            Some(AuthenticatedSession(accountId))
        }
      case _ => Some(NonAuthenticatedSession())
    }
  }
}
