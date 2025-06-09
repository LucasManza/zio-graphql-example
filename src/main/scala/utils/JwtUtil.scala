package utils


import domain.models.AccountDomain.{Account, AccountId, Email}
import domain.models.AuthServiceError
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
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

  def decode(token: String): Either[AuthServiceError, (AccountId, Email)] = {
    Jwt.decode(token, secretKey, Seq(algo)).toEither
      .flatMap { jwt =>
        val accountIdRaw = jwt.subject.getOrElse("")
        val emailRaw = jwt.content

        if (accountIdRaw.nonEmpty && emailRaw.nonEmpty) {
          for {
            accountId <- AccountId.apply(accountIdRaw).toOption.toRight(AuthServiceError.InvalidCredentials)
            email <- Email.apply(emailRaw).toOption.toRight(AuthServiceError.InvalidCredentials)
          } yield accountId -> email
        } else Left(AuthServiceError.InvalidToken)
      }
      .left.map(_ => AuthServiceError.InvalidToken)
  }
}