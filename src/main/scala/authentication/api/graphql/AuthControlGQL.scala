package authentication.api.graphql

import account.domain.models.AccountDomain.{AccountId, Email}
import authentication.domain.models.AuthDomain.{AuthenticatedSession, Session}
import caliban.CalibanError.ExecutionError
import caliban.ResponseValue
import caliban.Value.StringValue
import caliban.execution.FieldInfo
import caliban.parsing.adt.Directive
import caliban.schema.Annotations.GQLDirective
import caliban.wrappers.Wrapper.FieldWrapper
import zio.query.ZQuery

object AuthControlGQL {
  class AuthDirective[T <: AccountId | Email](directiveName: String, attributeName: String) extends GQLDirective(
    Directive(
      directiveName,
      Map(attributeName -> StringValue(attributeName.toString))
    )
  )

  class HasAccountIdDirective extends
    AuthDirective(HasAccountIdDirective.directiveName, HasAccountIdDirective.attributeName)

  object HasAccountIdDirective {
    val directiveName = "hasAccountId"
    val attributeName = "accountId"
  }

  val accessControlWrapper: FieldWrapper[AuthenticatedSession] = {
    new FieldWrapper[AuthenticatedSession](wrapPureValues = true) {
      override def wrap[R1 <: AuthenticatedSession](query: ZQuery[R1, ExecutionError, ResponseValue], info: FieldInfo): ZQuery[R1, ExecutionError, ResponseValue] = {
        ZQuery.serviceWithQuery[AuthenticatedSession] { session =>
          val requiredAccountId = getAccountId(info)
          requiredAccountId match {
            case None => query
            case Some(accountId) =>
              if session.accountId != accountId then ZQuery.fail(ExecutionError(s"Authentication required!"))
              else query
          }
        }
      }
    }
  }

  val accessControlWrapper2: FieldWrapper[Session] = new FieldWrapper[Session](wrapPureValues = true) {
    override def wrap[R1 <: Session](query: ZQuery[R1, ExecutionError, ResponseValue], info: FieldInfo): ZQuery[R1, ExecutionError, ResponseValue] = {
      query
    }
  }

  private def getAccountId(info: FieldInfo): Option[AccountId] = {
    info.directives
      .find(_.name == HasAccountIdDirective.directiveName)
      .flatMap(_.arguments.get(HasAccountIdDirective.attributeName))
      .flatMap {
        case StringValue(accountIdRaw) => AccountId.apply(accountIdRaw).toOption
        case _ => None
      }
  }
}


