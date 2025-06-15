package authentication.domain

import account.domain.AccountService
import account.domain.models.AccountDomain.{Email, Password}
import authentication.domain.AuthenticationServiceError.InvalidCredentials
import authentication.domain.models.AuthDomain.{AuthenticatedSession, JWTToken}
import utils.JwtUtil
import zio.*
import zio.stm.*

trait AuthenticationService {
  def login(email: Email, password: Password): IO[AuthenticationServiceError, JWTToken]
}

case class AuthenticationServiceLive private(accountService: AccountService) extends AuthenticationService {
  override def login(email: Email, password: Password): IO[AuthenticationServiceError, JWTToken] = {
    for {
      account <- accountService
        .getAccountByEmail(email).orElseFail(InvalidCredentials)
      result <-
        if account.password == password then
          ZIO.succeed(JWTToken(JwtUtil.encode(account)))
        else ZIO.fail(InvalidCredentials)
    } yield result
  }
}

object AuthenticationServiceLive {


  val layer: ZLayer[AccountService, Nothing, AuthenticationService] = ZLayer {
    for {
      accountService <- ZIO.service[AccountService]
    } yield new AuthenticationServiceLive(accountService)
  }

}