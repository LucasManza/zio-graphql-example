package authentication

import authentication.domain.models.AuthDomain.{AuthenticatedSession, NonAuthenticatedSession, Session}
import utils.JwtUtil
import zio.*
import zio.http.*


object AuthMiddleware {
  //  val middleware: HandlerAspect[Any, AuthenticatedSession] = Middleware.customAuthProviding { (request: Request) =>
  //    request.headers.get(Header.Authorization) match {
  //      case Some(Header.Authorization.Bearer(token)) =>
  //        JwtUtil.decode(token.value.toString) match {
  //          case Left(_) =>
  //            None
  //          case Right((accountId, email)) =>
  //            Some(AuthenticatedSession(accountId, email))
  //        }
  //      case _ => None
  //    }
  //  }
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
