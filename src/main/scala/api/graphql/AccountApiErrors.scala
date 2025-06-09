package api.graphql

import caliban.CalibanError
import domain.models.AccountServiceErrors.*

object AccountApiErrors {
  type AccountResponseError = CalibanError

  def handleError(serviceError: AccountServiceError): AccountResponseError = {
    serviceError match {
      case notFound: AccountNotFoundById => CalibanError.ExecutionError(s"Account not found by Id: ${notFound.accountId}")
      case notFound: AccountNotFoundByEmail => CalibanError.ExecutionError(s"Account not found by Id: ${notFound.email}")
      case duplicate: DuplicateAccount => CalibanError.ExecutionError(s"Already existed with email: ${duplicate.email}")
    }
  }
}
