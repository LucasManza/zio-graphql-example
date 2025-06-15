package authentication.domain.models

import account.domain.models.AccountDomain.{AccountId, Email}
import caliban.schema.Schema

object AuthDomain {
  final case class JWTToken(token: String)

  object JWTToken {
    given Schema[Any, JWTToken] = Schema.gen[Any, JWTToken]

  }

  sealed trait Session

  final case class NonAuthenticatedSession() extends Session

  final case class AuthenticatedSession(accountId: AccountId) extends Session

  object AuthenticatedSession {
    given Schema[Any, AuthenticatedSession] = Schema.gen[Any, AuthenticatedSession]
  }
}
