package domain.models

import caliban.CalibanError.ExecutionError
import caliban.schema.Schema
import domain.models.AccountDomain.{AccountId, Email}
import zio.IO

object AccountServiceErrors {
  type AccountServiceError =
    AccountNotFoundByEmail | AccountNotFoundById | DuplicateAccount

  case class AccountNotFoundById(accountId: AccountId)

  object AccountNotFoundById {
    given customEffectSchema[A](using s: Schema[Any, A]): Schema[Any, IO[AccountNotFoundById, A]] = {
      //Given a custom error. But options limitate it to Execution, Parsing and Validation.
      Schema.customErrorEffectSchema((notFound: AccountNotFoundById) => ExecutionError(s"Error account not found by Id: ${notFound.accountId}"))
    }
  }

  case class AccountNotFoundByEmail(email: Email)

  case class DuplicateAccount(email: Email)

}
