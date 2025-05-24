package domain.models

import caliban.CalibanError.ExecutionError
import caliban.Value
import caliban.schema.Schema.auto.*
import caliban.schema.{ArgBuilder, Schema, SchemaDerivation}


object AccountDomain {
  // Opaque type for AccountId
  opaque type AccountId = String

  object AccountIdSchema extends SchemaDerivation[AccountId]

  object AccountId {
    def apply(id: String): Either[String, AccountId] =
      if id.length >= 6 then Right(id)
      else Left("Account id is smaller than 6 characters")

    extension (a: AccountId) def value: String = a

    given Schema[Any, AccountId] = Schema.stringSchema.contramap(_.value)

    given ArgBuilder[AccountId] = {
      case Value.StringValue(value) =>
        AccountId.apply(value) match {
          case Right(value) => Right(value)
          case Left(reason) => Left(ExecutionError(s"Can't parse $value into a AccountId. Reason: $reason", innerThrowable = None))
        }
      case other => Left(ExecutionError(s"Can't build a AccountId from input $other"))
    }
  }


  case class Account(
                      id: AccountId,
                      email: String,
                      password: String
                    )

  object Account {

    given Schema[Any, Account] = Schema.gen[Any, Account]
  }
}
