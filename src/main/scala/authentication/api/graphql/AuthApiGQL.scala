package authentication.api.graphql

import account.domain.AccountService
import account.domain.models.AccountServiceErrors
import account.api.graphql.AccountApiErrors.AccountResponseError
import AuthApiGQL.LoginArgs.*
import account.domain.models.AccountDomain.{Email, Password}
import authentication.domain.models.AuthDomain.{AuthenticatedSession, JWTToken}
import authentication.domain.{AuthenticationServiceError, AuthenticationService}
import caliban.*
import caliban.schema.Annotations.GQLDescription
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import zio.{IO, Task, URLayer, ZIO, ZLayer}


object AuthApiGQL extends GenericSchema[AuthenticationService] {
  import auto.*

  

  // GraphQL API
  case class Queries()


  case class LoginArgs(email: Email, password: Password)

  case class Mutations(
                        @GQLDescription("Update the account email. [PROTECTED]")
                        login: LoginArgs => IO[Throwable, JWTToken]
                      )

  def handleError(error: AuthenticationServiceError): CalibanError = {
    error match {
      case AuthenticationServiceError.InvalidCredentials => CalibanError.ExecutionError("Invalid Crendentials!")
      case AuthenticationServiceError.InvalidToken => CalibanError.ExecutionError("Invalid Token!")
    }
  }

  object Mutations {

    val layer: URLayer[AuthenticationService, Mutations] = ZLayer {
      for {
        authService <- ZIO.service[AuthenticationService]
      } yield Mutations(
        login = args => authService.login(args.email, args.password).mapError(handleError)
      )
    }
  }

  val api =
    for {
      authService <- ZIO.service[AuthenticationService]
      mutations = Mutations(
        login = args => authService.login(args.email, args.password).mapError(handleError)
      )
    } yield graphQL(RootResolver(Queries(), mutations))
}
