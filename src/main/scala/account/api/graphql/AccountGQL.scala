package account.api.graphql

import account.api.graphql.AccountApiErrors.AccountResponseError
import account.domain.AccountService
import account.domain.models.AccountDomain.*
import authentication.api.graphql.AuthApiGQL.auto
import authentication.domain.SessionService
import authentication.domain.models.AuthDomain.AuthenticatedSession
import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, Schema}
import zio.{IO, Task, ZIO}

object AccountGQL {

  import auto.*


  // Queries

  case class GetAccountByEmailArgs(email: Email)

  case class Queries(
                      @GQLDescription("Get the account by email")
                      getAccountByEmail: GetAccountByEmailArgs => IO[AccountResponseError, Account],
                      @GQLDescription("Get the account by email")
                      getAllAccounts: Task[List[Account]]
                    )

  // Mutations

  case class CreateAccountArgs(email: Email, password: Password)


  case class UpdateEmailArgs(
                              newEmail: Email
                            )


  case class Mutations(
                        @GQLDescription("Update the account email. [PROTECTED]")
                        updateEmail: UpdateEmailArgs => IO[AccountResponseError, Account],
                        @GQLDescription("Create account with valid email and password ")
                        createAccount: CreateAccountArgs => IO[AccountResponseError, Account]
                      )

  // API

  val api = for {
    accountService <- ZIO.service[AccountService]
    sessionService <- ZIO.service[SessionService]
    queries = Queries(
      getAccountByEmail = args => accountService.getAccountByEmail(args.email).mapError(AccountApiErrors.handleError),
      getAllAccounts = accountService.getAllAccounts
    )
    mutations = Mutations(
      createAccount = args => accountService.createAccount(args.email, args.password).mapError(AccountApiErrors.handleError),
      updateEmail = args =>
        for {
          accountId <- sessionService.getAuthenticatedSession.flatMap {
            case Some(AuthenticatedSession(accountId)) => ZIO.succeed(accountId)
            case None => ZIO.fail(CalibanError.ExecutionError(s"Not Authenticated!"))
          }.orElseFail(CalibanError.ExecutionError(s"Unexpectedly failed!"))
          _ <- ZIO.succeed(println(s"AccountID: $accountId"))
          result <- accountService.updateEmail(accountId, args.newEmail).mapError(AccountApiErrors.handleError)
        } yield result
    )
  } yield graphQL(RootResolver(queries, mutations))

}
