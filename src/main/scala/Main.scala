import account.api.graphql.AccountGQL
import account.domain.MockAccountService
import authentication.AuthMiddleware
import authentication.api.graphql.AuthApiGQL
import authentication.domain.AuthenticationServiceLive
import authentication.domain.models.AuthDomain.NonAuthenticatedSession
import caliban.*
import caliban.CalibanError.*
import caliban.Value.StringValue
import caliban.execution.FieldInfo
import caliban.parsing.adt.Directive
import caliban.quick.*
import caliban.schema.Annotations.*
import caliban.schema.Schema
import caliban.wrappers.Wrapper.FieldWrapper
import zio.*
import zio.http.*

import scala.util.Try

object Main extends ZIOAppDefault {


  override def run = {
    for {
      authApi <- AuthApiGQL.api
      accountApi <- AccountGQL.api
      protectedRoutes <- (accountApi |+| authApi)
        .routes(
          apiPath = "/api/graphql",
          graphiqlPath = Some("/api/graphiql")
        ).map(_ @@ AuthMiddleware.middleware2)
      port <- Server.install(protectedRoutes)
      _ <- ZIO.logInfo(s"Server started on port $port")
      _ <- ZIO.never
    } yield ()
  }.provide(
    MockAccountService.layer,
    AuthenticationServiceLive.layer,
    ZLayer.succeed(NonAuthenticatedSession()),
    Server.defaultWithPort(8088)
  )

}
