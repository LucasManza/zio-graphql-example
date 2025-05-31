package domain.models

import caliban.schema.Schema
import domain.models.AccountDomain.{Account, AccountId, Email}

object AccountResponses {
  enum GetAccountByEmailResponse derives Schema.Auto {
    case AccountFound(account: Account)
    case AccountByEmailNotFound(invalidEmail: Email)
  }

  enum CreateAccountResponse derives Schema.Auto {
    case AccountCreated(account: Account)
    case AlreadyRegistered(invalidEmail: Email)
  }

  enum UpdateEmailResponse {
    case AccountUpdated(account: Account)
    case AccountByIdNotFound(invalidId: AccountId)
  }

  object UpdateEmailResponse {

    given Schema[Any, UpdateEmailResponse] = Schema.gen[Any, UpdateEmailResponse]

  }
}
