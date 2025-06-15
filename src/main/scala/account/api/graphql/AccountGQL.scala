package account.api.graphql

import account.api.graphql.AccountApiErrors.AccountResponseError
import account.domain.AccountService
import account.domain.models.AccountDomain.*
import authentication.api.graphql.AuthApiGQL.auto
import authentication.api.graphql.AuthControlGQL.{HasAccountIdDirective, accessControlWrapper, accessControlWrapper2}
import authentication.domain.models.AuthDomain.{AuthenticatedSession, NonAuthenticatedSession, Session}
import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import zio.{IO, Task, ZIO}

import scala.compiletime.ops.boolean.&&

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
                              //                              @HasAccountIdDirective accountId: AccountId,
                              newEmail: Email
                            )


  case class Mutations(
                        @GQLDescription("Update the account email. [PROTECTED]")
                        updateEmail: UpdateEmailArgs => IO[CalibanError, Account],
                        @GQLDescription("Create account with valid email and password ")
                        createAccount: CreateAccountArgs => IO[AccountResponseError, Account]
                      )

  // API

  val api = for {
    accountService <- ZIO.service[AccountService]
    //    authenticatedService <- ZIO.service[AuthenticationSessionService]
    session <- ZIO.service[Session]
    queries = Queries(
      getAccountByEmail = args => accountService.getAccountByEmail(args.email).mapError(AccountApiErrors.handleError),
      getAllAccounts = accountService.getAllAccounts
    )
    mutations = Mutations(
      createAccount = args => accountService.createAccount(args.email, args.password).mapError(AccountApiErrors.handleError),
      updateEmail = args =>
        for {
          accountId <- session match {
            case NonAuthenticatedSession() => ZIO.fail(CalibanError.ExecutionError(s"Not Authenticated!"))
            case AuthenticatedSession(accountId) => ZIO.succeed(accountId)
          }
          //          authSession <- authenticatedService.getAuthSession.orElseFail(CalibanError.ExecutionError(s"Not Authenticated!")) //Here I want to get the AuthSession from AuthSessionService
          result <- accountService.updateEmail(accountId, args.newEmail).mapError(AccountApiErrors.handleError)
        } yield result
    )
  } yield graphQL(RootResolver(queries, mutations)) @@ accessControlWrapper2

}
