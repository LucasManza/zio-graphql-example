package api.graphql

import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import domain.AccountService
import domain.models.AccountDomain.*
import domain.models.AccountDomain.AccountIdSchema.auto
import zio.{UIO, ZIO, ZLayer}

object AccountGQL extends GenericSchema[AccountService] {

  import domain.models.AccountDomain.AccountIdSchema.auto.*

  // GraphQL API
  case class GetAccountByIdArgs(accountId: AccountId)

  case class Queries(
                      @GQLDescription("Get the account")
                      getAccountById: GetAccountByIdArgs => UIO[Option[Account]]
                    )


  case class UpdateEmailArgs(accountId: AccountId, newEmail: String)

  case class Mutations(
                        @GQLDescription("Update the account email")
                        updateEmail: UpdateEmailArgs => UIO[Option[Account]]
                      )


  val apiLayer = ZLayer {
    for {
      service <- ZIO.service[AccountService]
    } yield
      graphQL(
        RootResolver(
          Queries(
            getAccountById = args => service
              .getAccount(args.accountId)
              .map(Some(_))
              .catchAll(_ => ZIO.succeed(None))
          ),
          Mutations(
            updateEmail = args => service
              .updateEmail(args.accountId, args.newEmail)
              .map(Some(_))
              .catchAll(_ => ZIO.succeed(None))
          )
        )
      )
  }
}
