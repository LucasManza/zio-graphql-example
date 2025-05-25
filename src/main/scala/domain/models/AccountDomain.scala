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
          case Left(reason) => Left(ExecutionError(s"Can't parse '$value' into a AccountId. Reason: $reason", innerThrowable = None))
        }
      case other => Left(ExecutionError(s"Can't build AccountId from input $other"))
    }
  }


  opaque type Email = String

  object Email {
    def apply(email: String): Either[String, Email] = {
      if email.matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
      then Right(email)
      else Left(s"Input $email is not a email!")
    }

    extension (a: Email) def value: String = a

    given Schema[Any, Email] = Schema.stringSchema.contramap(_.value)

    given ArgBuilder[Email] = {
      case Value.StringValue(value) =>
        Email.apply(value) match {
          case Right(value) => Right(value)
          case Left(reason) => Left(ExecutionError(s"Can't parse '$value' into a Email. Reason: $reason", innerThrowable = None))
        }
      case other => Left(ExecutionError(s"Can't build Email from input $other"))
    }
  }

  opaque type Password = String

  object Password {
    def apply(password: String): Either[String, Password] = {
      if password.length >= 6 then Right(password)
      else Left(s"Input $password is not a password!")
    }

    extension (a: Password) def value: String = a

    given Schema[Any, Password] = Schema.stringSchema.contramap(_.value)

    given ArgBuilder[Password] = {
      case Value.StringValue(value) =>
        Password.apply(value) match {
          case Right(value) => Right(value)
          case Left(reason) => Left(ExecutionError(s"Can't parse '$value' into Password. Reason: $reason", innerThrowable = None))
        }
      case other => Left(ExecutionError(s"Can't build Password from input $other"))
    }
  }

  case class Account(
                      id: AccountId,
                      email: Email,
                      password: Password
                    )

  object Account {

    given Schema[Any, Account] = Schema.gen[Any, Account]
  }
}
