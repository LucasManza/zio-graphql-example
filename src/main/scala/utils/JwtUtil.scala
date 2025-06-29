package utils


import account.domain.models.AccountDomain.{Account, AccountId}
import authentication.domain.AuthenticationServiceError
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import java.time.Clock

object JwtUtil {
  given Clock = Clock.systemUTC()

  private val secretKey: String = "super_secret_key_please_change"
  private val algo = JwtAlgorithm.HS256


  def encode(account: Account): String = {
    val claim = JwtClaim(
      content = account.email.toString,
      subject = Some(account.id.toString)
    ).issuedNow.expiresIn(60 * 60 * 24)
    Jwt.encode(claim, secretKey, algo)
  }

  def decode(token: String): Either[AuthenticationServiceError, (AccountId)] = {
    Jwt.decode(token, secretKey, Seq(algo)).toEither
      .flatMap { jwt =>
        val accountIdRaw = jwt.subject.getOrElse("")

        if (accountIdRaw.nonEmpty) {
          for {
            accountId <- AccountId.apply(accountIdRaw).toOption.toRight(AuthenticationServiceError.InvalidCredentials)
          } yield accountId
        } else Left(AuthenticationServiceError.InvalidToken)
      }
      .left.map(_ => AuthenticationServiceError.InvalidToken)
  }
}