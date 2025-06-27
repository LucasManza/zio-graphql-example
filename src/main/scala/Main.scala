import account.api.graphql.AccountGQL
import account.domain.MockAccountService
import authentication.CustomMiddleware
import authentication.api.graphql.AuthApiGQL
import authentication.domain.models.AuthDomain.NonAuthenticatedSession
import authentication.domain.{AuthenticationServiceLive, SessionServiceLive}
import caliban.*
import caliban.quick.*
import zio.*
import zio.http.*

object Main extends ZIOAppDefault {


  override def run = {
    for {
      authApi <- AuthApiGQL.api
      accountApi <- AccountGQL.api
      protectedRoutes <- (accountApi |+| authApi)
        .routes(
          apiPath = "/api/graphql",
          graphiqlPath = Some("/api/graphiql")
        ).map(_ @@ CustomMiddleware.authMiddleware)
      port <- Server.install(protectedRoutes)
      _ <- ZIO.logInfo(s"Server started on port $port")
      _ <- ZIO.never
    } yield ()
  }.provide(
    MockAccountService.layer,
    AuthenticationServiceLive.layer,
    SessionServiceLive.layer,
    Server.defaultWithPort(8088)
  )

}
