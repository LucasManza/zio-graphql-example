package api.graphql

import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import domain.AccountService
import domain.models.AccountDomain.*
import domain.models.AccountDomain.AccountIdSchema.auto
import domain.models.AccountResponses.*
import domain.models.AccountResponses.CreateAccountResponse.*
import domain.models.AccountResponses.GetAccountByEmailResponse.*
import domain.models.AccountResponses.UpdateEmailResponse.*
import domain.models.AccountServiceErrors
import domain.models.AccountServiceErrors.*
import zio.{IO, Task, URLayer, ZIO, ZLayer}

object AccountGQL extends GenericSchema[AccountService] {

  import domain.models.AccountDomain.AccountIdSchema.auto.*

  // GraphQL API
  case class GetAccountByIdArgs(accountId: AccountId)

  case class GetAccountByEmailArgs(email: Email)

  case class Queries(
                      @GQLDescription("Get the account by id")

                      /**
                       * Queries must fail with a Throwable or at least CalibanError (extends from Throwable)
                       * That's why we can have effect like Task with some content.
                       * [[AccountServiceErrors.AccountNotFoundById]] is a custom caliban error but a basic one check.
                       * GraphQL Recommendation is parsing the errors into ExecutionErrors for business logic errors:
                       * `error match
                       *    case notFound => ExecutionError(
                       *        msg = err.message,
                       *        path = Nil,
                       *        locations = Nil,
                       *        extensions = Some(
                       *          ResponseValue.ObjectValue(
                       *            List(
                       *                "code" -> ResponseValue.StringValue(err.code)
                       *            )
                        *         )
                       *    )
                       * `
                       */
                      getAccountById: GetAccountByIdArgs => IO[AccountServiceErrors.AccountNotFoundById, Account],
                      // This is an example with a flatten Response, instead of a custom error by moving the error channel to success
                      @GQLDescription("Get the account by email")
                      getAccountByEmail: GetAccountByEmailArgs => Task[GetAccountByEmailResponse],
                    )

  object Queries {

    val layer: URLayer[AccountService, Queries] = ZLayer {
      for {
        service <- ZIO.service[AccountService]
      } yield Queries(
        getAccountById = args => service.getAccountById(args.accountId),
        getAccountByEmail = args => service.getAccountByEmail(args.email).either.map {
          case Right(account) => AccountFound(account)
          case Left(notFoundByEmail: AccountServiceErrors.AccountNotFoundByEmail) => AccountByEmailNotFound(notFoundByEmail.email)
        }
      )
    }
  }


  case class CreateAccountArgs(email: Email, password: Password)

  case class UpdateEmailArgs(accountId: AccountId, newEmail: Email)


  case class Mutations(
                        @GQLDescription("Update the account email")
                        updateEmail: UpdateEmailArgs => Task[UpdateEmailResponse],
                        @GQLDescription("Create account with valids email and password ")
                        createAccount: CreateAccountArgs => Task[CreateAccountResponse]
                      )

  object Mutations {

    val layer: URLayer[AccountService, Mutations] = ZLayer {
      for {
        service <- ZIO.service[AccountService]
      } yield Mutations(
        // Case mapping the error to a custom Response and return a 200
        updateEmail = args => service.updateEmail(args.accountId, args.newEmail)
          .either
          .map {
            case Left(notFound: AccountServiceErrors.AccountNotFoundById) => AccountByIdNotFound(notFound.accountId)
            case Right(account) => AccountUpdated(account)
          },
        createAccount = args => service.createAccount(args.email, args.password)
          .either
          .map {
            case Left(duplicated: AccountServiceErrors.DuplicateAccount) => AlreadyRegistered(duplicated.email)
            case Right(account) => AccountCreated(account)
          }
      )
    }
  }


  val apiLayer =
    ZLayer {
      for {
        queries <- ZIO.service[Queries]
        mutations <- ZIO.service[Mutations]
      } yield
        graphQL(
          RootResolver(queries, mutations)
        )
    }

}
