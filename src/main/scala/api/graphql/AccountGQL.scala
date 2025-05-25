package api.graphql

import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import domain.AccountService
import domain.models.AccountDomain.*
import domain.models.AccountDomain.AccountIdSchema.auto
import domain.models.AccountErrors
import domain.models.AccountErrors.*
import domain.models.AccountResponse.*
import domain.models.AccountResponse.UpdateEmailResponse.{AccountNotFoundById, Updated}
import zio.{IO, Task, URLayer, ZIO, ZLayer}

object AccountGQL extends GenericSchema[AccountService] {

  import domain.models.AccountDomain.AccountIdSchema.auto.*

  // GraphQL API
  case class GetAccountByIdArgs(accountId: AccountId)

  case class Queries(
                      @GQLDescription("Get the account")

                      /**
                       * Queries must fail with a Throwable or at least CalibanError (extends from Throwable)
                       * That's why we can have effect like Task with some content.
                       * [[AccountErrors.AccountNotFound]] is a custom caliban error but a basic one check.
                       * Recommend it to move errors to the A ZIO's channel so they are mapped
                       */
                      getAccountById: GetAccountByIdArgs => IO[AccountErrors.AccountNotFound, Account]
                    )

  object Queries {

    val layer: URLayer[AccountService, Queries] = ZLayer {
      for {
        service <- ZIO.service[AccountService]
      } yield Queries(
        getAccountById = args => service.getAccountById(args.accountId)
      )
    }
  }


  case class UpdateEmailArgs(accountId: AccountId, newEmail: String)



  case class Mutations(
                        @GQLDescription("Update the account email")
                        updateEmail: UpdateEmailArgs => Task[UpdateEmailResponse]
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
            case Left(notFound: AccountErrors.AccountNotFound) => AccountNotFoundById(notFound.accountId)
            case Right(account) => Updated(account)
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
