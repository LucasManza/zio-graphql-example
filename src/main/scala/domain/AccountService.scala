package domain

import domain.models.AccountDomain.*
import domain.models.AccountErrors.*
import zio.{IO, ZIO}

// Service definition
trait AccountService {
  def getAccountById(accountId: AccountId): IO[AccountNotFound, Account]

  def updateEmail(accountId: AccountId, newEmail: String): IO[AccountNotFound, Account]
}

// Mock implementation
class MockAccountService extends AccountService {
  private val mockAccountId = AccountId.apply("acc-123").toOption.get
  private val accounts = scala.collection.concurrent.TrieMap[AccountId, Account](
    mockAccountId -> Account(mockAccountId, "account@email.com", "password")
  )

  def getAccountById(accountId: AccountId): IO[AccountNotFound, Account] = {
    accounts.get(accountId) match {
      case Some(account) => ZIO.succeed(account)
      case None => ZIO.fail(AccountNotFound(accountId))
    }
  }

  def updateEmail(accountId: AccountId, newEmail: String): IO[AccountNotFound, Account] = {
    for {
      account <- getAccountById(accountId)
      updated = account.copy(email = newEmail)
      result <- accounts.put(accountId, updated) match {
        case Some(_) => ZIO.succeed(updated)
        case None => ZIO.fail(AccountNotFound(accountId))
      }
    } yield result
  }
}