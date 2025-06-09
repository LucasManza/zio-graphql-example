package domain

import domain.models.AccountDomain.{Email, Password}
import domain.models.AuthDomain.JWTToken
import domain.models.AuthServiceError
import domain.models.AuthServiceError.InvalidCredentials
import utils.JwtUtil
import zio.*
import zio.stm.*

trait AuthService {
  def login(email: Email, password: Password): IO[AuthServiceError, JWTToken]
}

case class AuthServiceLive private(accountService: AccountService) extends AuthService {
  override def login(email: Email, password: Password): IO[AuthServiceError, JWTToken] = {
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

object AuthServiceLive {
  val layer: ZLayer[AccountService, Nothing, AuthService] = ZLayer {
    for {
      accountService <- ZIO.service[AccountService]
    } yield new AuthServiceLive(accountService)
  }

}