import api.graphql.AccountGQL
import caliban.*
import caliban.quick.*
import domain.{AccountService, MockAccountService}
import zio.*


object Main extends ZIOAppDefault {

  val accountServiceLayer = ZLayer.succeed(new MockAccountService: AccountService)

  override def run = {
    for {
      api <- ZIO.service[GraphQL[Any]]
      _ <- api.runServer(
        port = 8088,
        apiPath = "/api/graphql",
        graphiqlPath = Some("/api/graphiql")
      )
    } yield ()

  }.provide(accountServiceLayer, AccountGQL.apiLayer)

}
