package domain.models

import caliban.CalibanError.ExecutionError
import caliban.schema.Schema
import domain.models.AccountDomain.AccountId
import zio.IO

object AccountErrors {
  case class AccountNotFound(accountId: AccountId)

  object AccountNotFound {
    given customEffectSchema[A](using s: Schema[Any, A]): Schema[Any, IO[AccountNotFound, A]] = {
      //Given a custom error. But options are limitate it to Execution, Parsing and Validation.
      Schema.customErrorEffectSchema((notFound: AccountNotFound) => ExecutionError(s"Error account not found: ${notFound.accountId}"))
    }

  }

}
