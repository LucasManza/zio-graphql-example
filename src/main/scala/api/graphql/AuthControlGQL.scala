package api.graphql

import caliban.CalibanError.ExecutionError
import caliban.ResponseValue
import caliban.Value.StringValue
import caliban.execution.FieldInfo
import caliban.parsing.adt.Directive
import caliban.schema.Annotations.GQLDirective
import caliban.wrappers.Wrapper.FieldWrapper
import domain.models.AccountDomain.{AccountId, Email}
import domain.models.AuthDomain.AuthSession
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

  val accessControlWrapper: FieldWrapper[AuthSession] = {
    new FieldWrapper[AuthSession](wrapPureValues = true) {
      override def wrap[R1 <: AuthSession](query: ZQuery[R1, ExecutionError, ResponseValue], info: FieldInfo): ZQuery[R1, ExecutionError, ResponseValue] = {
        ZQuery.serviceWithQuery[AuthSession] { session =>
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


