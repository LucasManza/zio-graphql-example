package domain.models

import caliban.schema.Schema
import domain.models.AccountDomain.{Account, AccountId}

object AccountResponse {
  enum GetAccountByEmailResponse derives Schema.Auto {
    case Found(account: Account)
    case NotFound(email: String)
  }

  enum UpdateEmailResponse {
    case Updated(account: Account)
    case AccountNotFoundById(accountId: AccountId)
  }

  object UpdateEmailResponse {

    given Schema[Any, UpdateEmailResponse] = Schema.gen[Any, UpdateEmailResponse]

  }
}
