import api.graphql.{AccountGQL, AuthApiGQL}
import caliban.*
import caliban.quick.*
import domain.models.AuthDomain.AuthSession
import domain.{AccountService, AuthServiceLive, MockAccountService}
import zio.*
import zio.http.*

object Main extends ZIOAppDefault {
  //  override def run =
  //    Server.serve(routes).provide(
  //      AuthServiceLive.layer,
  //      MockAccountService.layer,
  //      Server.default
  //    )


  override def run = {
    for {
      accountApi <- AccountGQL.api
      authApi <- AuthApiGQL.api

      protectedRoutes <- (accountApi |+| authApi)
        .routes(
          apiPath = "/api/graphql",
          graphiqlPath = Some("/api/graphiql")
        )
        .map(_ @@ AuthMiddleware.middleware)
      port <- Server.install(protectedRoutes)
      _ <- ZIO.logInfo(s"Server started on port $port")
      _ <- ZIO.never
    } yield ()
  }
    .provide(
      MockAccountService.layer,
      AuthServiceLive.layer,
      Server.defaultWithPort(8088)
    )

}
