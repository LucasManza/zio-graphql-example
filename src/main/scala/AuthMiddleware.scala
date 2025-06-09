import domain.models.AuthDomain.AuthSession
import utils.JwtUtil
import zio.*
import zio.http.*


object AuthMiddleware {
  val middleware: HandlerAspect[Any, AuthSession] = Middleware.customAuthProviding { (request: Request) =>
    request.headers.get(Header.Authorization) match {
      case Some(Header.Authorization.Bearer(token)) =>
        JwtUtil.decode(token.value.toString) match {
          case Left(_) =>
            None
          case Right((accountId, email)) =>
            Some(AuthSession(accountId, email))
        }
      case _ => None
    }
  }


}
