package domain

import domain.models.AccountDomain.*
import zio.{IO, ZIO}

// Service definition
trait AccountService {
  def getAccount(accountId: AccountId): IO[String, Account]

  def updateEmail(accountId: AccountId, newEmail: String): IO[String, Account]
}

// Mock implementation
class MockAccountService extends AccountService {
  private val mockAccountId = AccountId.apply("acc-123").toOption.get
  private var accounts = scala.collection.concurrent.TrieMap[AccountId, Account](
    mockAccountId -> Account(mockAccountId, "account@email.com", "password")
  )

  def getAccount(accountId: AccountId): IO[String, Account] = {
    accounts.get(accountId) match {
      case Some(account) => ZIO.succeed(account)
      case None => ZIO.fail("Account not found!")
    }
  }

  def updateEmail(accountId: AccountId, newEmail: String): IO[String, Account] = {
    for {
      account <- getAccount(accountId)
      updated = account.copy(email = newEmail)
      result <- accounts.put(accountId, updated) match {
        case Some(_) => ZIO.succeed(updated)
        case None => ZIO.fail("Account not found!")
      }
    } yield result
  }
}