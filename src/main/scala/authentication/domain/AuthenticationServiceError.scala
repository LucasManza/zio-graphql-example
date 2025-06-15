package authentication.domain

enum AuthenticationServiceError {
  case InvalidCredentials, InvalidToken
  case AuthenticationRequired
}
