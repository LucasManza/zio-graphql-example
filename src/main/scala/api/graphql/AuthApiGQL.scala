package api.graphql

import api.graphql.AccountApiErrors.AccountResponseError
import api.graphql.AccountGQL.auto
import api.graphql.AuthApiGQL.LoginArgs.*
import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import domain.models.AccountDomain.*
import domain.models.AccountServiceErrors.*
import domain.models.AuthDomain.{AuthSession, JWTToken}
import domain.models.{AccountServiceErrors, AuthServiceError}
import domain.{AccountService, AuthService}
import zio.{IO, Task, URLayer, ZIO, ZLayer}


object AuthApiGQL extends GenericSchema[AuthService] {
  import auto.*

  

  // GraphQL API
  case class Queries()


  case class LoginArgs(email: Email, password: Password)

  case class Mutations(
                        @GQLDescription("Update the account email. [PROTECTED]")
                        login: LoginArgs => IO[Throwable, JWTToken]
                      )

  def handleError(error: AuthServiceError): CalibanError = {
    error match {
      case AuthServiceError.InvalidCredentials => CalibanError.ExecutionError("Invalid Crendentials!")
      case AuthServiceError.InvalidToken => CalibanError.ExecutionError("Invalid Token!")
    }
  }

  object Mutations {

    val layer: URLayer[AuthService, Mutations] = ZLayer {
      for {
        authService <- ZIO.service[AuthService]
      } yield Mutations(
        login = args => authService.login(args.email, args.password).mapError(handleError)
      )
    }
  }

  val api =
    for {
      authService <- ZIO.service[AuthService]
      mutations = Mutations(
        login = args => authService.login(args.email, args.password).mapError(handleError)
      )
    } yield graphQL(RootResolver(Queries(), mutations))
}
