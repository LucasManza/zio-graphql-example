import GraphQLJsonCodec.graphQLRequestEncoder
import account.domain.models.AccountDomain.{Account, AccountId, Email, Password}
import account.domain.{AccountService, MockAccountService}
import authentication.domain.{AuthenticationServiceLive, SessionServiceLive}
import caliban.Value.StringValue
import caliban.interop.zio.ZioJson
import caliban.{GraphQLRequest, GraphQLResponse}
import utils.JwtUtil
import zio.*
import zio.http.*
import zio.json.*
import zio.test.*

object ConcurrentEmailUpdateSpec extends ZIOSpecDefault {

  // Define 3 test accounts
  val accounts = List(
    Account(
      id = AccountId.apply("acc-001").toOption.get,
      email = Email.apply("user1@test.com").toOption.get,
      password = Password.apply("password1").toOption.get
    ),
    Account(
      id = AccountId.apply("acc-002").toOption.get,
      email = Email.apply("user2@test.com").toOption.get,
      password = Password.apply("password2").toOption.get
    ),
    Account(
      id = AccountId.apply("acc-003").toOption.get,
      email = Email.apply("user3@test.com").toOption.get,
      password = Password.apply("password3").toOption.get
    )
  )

  // Generate JWT tokens for each account
  val tokens: List[String] = accounts.map(JwtUtil.encode)

  // New emails for update
  val newEmails = List("new1@test.com", "new2@test.com", "new3@test.com")

  val updateEmailMutation =
    """
      mutation UpdateEmail($newEmail: String!) {
          updateEmail(newEmail: $newEmail) {
              id
              email
          }
      }
    """

  val getAllAccountsQuery =
    """
      query GetAllAccounts {
          getAllAccounts {
              id
              email
          }
      }
    """


  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ConcurrentEmailUpdateSpec")(
      test("3 users can concurrently update their own emails") {
        // Helper to send GraphQL requests with Authorization header
        def sendGraphQLRequest(
                                request: GraphQLRequest,
                                url: URL,
                                authorizationHeader: Option[String] = None
                              ): ZIO[Client, Throwable, String] = {
          val jsonBody = request.toJson
          val httpReq = Request
            .post(url, Body.fromString(jsonBody))
            .addHeader(Header.ContentType(MediaType.application.json))
            .addHeader(Header.Accept(MediaType.application.json))
          val finalHttpReq = authorizationHeader.fold(httpReq)(auth => httpReq.addHeader(Header.Authorization.Bearer(auth)))
          for {
            response <- Client.request(finalHttpReq)
            //      responseBody <- response.body.asString
            //      graphQLResponse <- ZIO.fromEither(responseBody.toJson[GraphQLResponse]).mapError(e => new Throwable(s"Failed to parse GraphQL response: $e\nBody: $responseBody"))
            responseBody <- response.body.asString
          } yield responseBody
        }.provide(ZClient.default, Scope.default)


        val graphqlUrl = URL.decode("http://localhost:8088/api/graphql").toOption.get
        val updateRequests = accounts.zip(tokens).zip(newEmails).map {
          case ((account, token), newEmail) =>
            (
              GraphQLRequest(
                query = Some(updateEmailMutation),
                operationName = Some("UpdateEmail"),
                variables = Some(Map("newEmail" -> StringValue(newEmail)))
              ),
              token,
              account.id.value,
              newEmail
            )
        }
        for {
          accountService <- ZIO.service[AccountService]
//          // Ensure accounts exist in the mock service
//          _ <- ZIO.foreach(updateRequests) { case (_, _, id, _) =>
//            accountService.addAccount(
//              Account(
//                id = AccountId.apply(id).toOption.get,
//                email = Email.apply(s"$id@init.com").toOption.get,
//                password = Password.apply("password").toOption.get
//              )
//            )
//          }
          // Run concurrent updateEmail mutations
          _ <- ZIO.foreachPar(updateRequests) { case (req, token, _, _) =>
            sendGraphQLRequest(req, graphqlUrl, Some(token))
          }
          // Fetch all accounts
          allAccountsResp <- sendGraphQLRequest(
            GraphQLRequest(query = Some(getAllAccountsQuery), operationName = Some("GetAllAccounts")),
            graphqlUrl
          )
          // Parse and assert
          //          data = allAccountsResp.data
          emailsUpdated = newEmails.forall { newEmail =>
            allAccountsResp.contains(newEmail)
          }
        } yield assertTrue(emailsUpdated)
      }
    ).provide(
      MockAccountService.layer,
      ZClient.default,
    )
  }
}