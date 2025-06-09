package domain.models

import caliban.schema.Schema
import domain.models.AccountDomain.{AccountId, Email}

object AuthDomain {
  final case class JWTToken(token: String)

  object JWTToken {
    given Schema[Any, JWTToken] = Schema.gen[Any, JWTToken]

  }

  final case class AuthSession(accountId: AccountId, email: Email)

  object AuthSession {
    given Schema[Any, AuthSession] = Schema.gen[Any, AuthSession]
  }
}
