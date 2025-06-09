package domain

import domain.models.AccountDomain.*
import domain.models.AccountServiceErrors.*
import zio.{IO, ZIO, ZLayer}

import scala.util.Random

// Service definition
trait AccountService {
  def getAccountById(accountId: AccountId): IO[AccountNotFoundById, Account]

  def getAccountByEmail(email: Email): IO[AccountNotFoundByEmail, Account]

  def updateEmail(accountId: AccountId, newEmail: Email): IO[AccountNotFoundById, Account]

  def createAccount(email: Email, password: Password): IO[DuplicateAccount, Account]
}

// Mock implementation
case class MockAccountService() extends AccountService {
  private val mockAccountId = AccountId.apply("acc-123").toOption.get
  private val mockAccountEmail = Email.apply("account@gmail.com").toOption.get
  private val mockAccountPassword = Password.apply("password").toOption.get
  private val accounts = scala.collection.concurrent.TrieMap[AccountId, Account](
    mockAccountId -> Account(mockAccountId, mockAccountEmail, mockAccountPassword)
  )

  override def getAccountById(accountId: AccountId): IO[AccountNotFoundById, Account] = {
    accounts.get(accountId) match {
      case Some(account) => ZIO.succeed(account)
      case None => ZIO.fail(AccountNotFoundById(accountId))
    }
  }

  override def updateEmail(accountId: AccountId, newEmail: Email): IO[AccountNotFoundById, Account] = {
    for {
      account <- getAccountById(accountId)
      updated = account.copy(email = newEmail)
      result <- accounts.put(accountId, updated) match {
        case Some(_) => ZIO.succeed(updated)
        case None => ZIO.fail(AccountNotFoundById(accountId))
      }
    } yield result
  }

  override def getAccountByEmail(email: Email): IO[AccountNotFoundByEmail, Account] =
    accounts.find(a => a._2.email == email) match {
      case Some((_, account)) => ZIO.succeed(account)
      case None => ZIO.fail(AccountNotFoundByEmail(email))
    }

  override def createAccount(email: Email, password: Password): IO[DuplicateAccount, Account] = {
    for {
      _ <- getAccountByEmail(email).flip.orElseFail(DuplicateAccount(email))
      accountId = AccountId.apply(RandomStringGenerator.randomAlphanumeric()).toOption.get
      newAccount = Account(accountId, email, password)
      result <-
        if accounts.contains(newAccount.id) then ZIO.fail(DuplicateAccount(email))
        else {
          accounts += newAccount.id -> newAccount
          ZIO.succeed(newAccount)
        }
    } yield result
  }

}

object MockAccountService {
  val layer = ZLayer.succeed(new MockAccountService: AccountService)
}

object RandomStringGenerator {

  private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

  def randomAlphanumeric(length: Int = 6): String =
    (1 to length).map(_ => chars(Random.nextInt(chars.length))).mkString

  // Example usage
  def main(args: Array[String]): Unit = {
    println(randomAlphanumeric()) // e.g., "f3Z9aB"
  }
}