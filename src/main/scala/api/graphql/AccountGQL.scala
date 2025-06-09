package api.graphql

import api.graphql.AccountApiErrors.AccountResponseError
import api.graphql.AuthControlGQL.{HasAccountIdDirective, accessControlWrapper}
import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import domain.AccountService
import domain.models.AccountDomain.*
import domain.models.AccountServiceErrors.*
import zio.{IO, ZIO}

object AccountGQL extends GenericSchema[AccountService] {

  import auto.*


  // Queries

  case class GetAccountByEmailArgs(email: Email)

  case class Queries(
                      @GQLDescription("Get the account by email")
                      getAccountByEmail: GetAccountByEmailArgs => IO[AccountResponseError, Account]
                    )

  // Mutations

  case class CreateAccountArgs(email: Email, password: Password)


  case class UpdateEmailArgs(
                              @HasAccountIdDirective accountId: AccountId,
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
    queries = Queries(
      getAccountByEmail = args => accountService.getAccountByEmail(args.email).mapError(AccountApiErrors.handleError)
    )
    mutations = Mutations(
      createAccount = args => accountService.createAccount(args.email, args.password).mapError(AccountApiErrors.handleError),
      updateEmail = args => accountService.updateEmail(args.accountId, args.newEmail).mapError(AccountApiErrors.handleError)
    )
  } yield graphQL(RootResolver(queries, mutations)) @@ accessControlWrapper

}
