package account.domain

import account.domain.models.AccountDomain.*
import account.domain.models.AccountServiceErrors.*
import zio.{IO, Task, ZIO, ZLayer}

import scala.util.Random

// Service definition
trait AccountService {
  def getAccountById(accountId: AccountId): IO[AccountNotFoundById, Account]

  def getAccountByEmail(email: Email): IO[AccountNotFoundByEmail, Account]

  def getAllAccounts: Task[List[Account]]

  def updateEmail(accountId: AccountId, newEmail: Email): IO[AccountNotFoundById, Account]

  def createAccount(email: Email, password: Password): IO[DuplicateAccount, Account]

  def addAccount(newAccount: Account): IO[DuplicateAccount, Account]
}

// Mock implementation
case class MockAccountService() extends AccountService {
  private val accounts = List(
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

  private val dummyRepo: scala.collection.concurrent.TrieMap[AccountId, Account] =
    accounts.foldLeft(new scala.collection.concurrent.TrieMap[AccountId, Account]()) { (acc, account) =>
      acc.put(account.id, account)
      acc
    }


  override def getAccountById(accountId: AccountId): IO[AccountNotFoundById, Account] = {
    dummyRepo.get(accountId) match {
      case Some(account) => ZIO.succeed(account)
      case None => ZIO.fail(AccountNotFoundById(accountId))
    }
  }

  override def updateEmail(accountId: AccountId, newEmail: Email): IO[AccountNotFoundById, Account] = {
    for {
      account <- getAccountById(accountId)
      updated = account.copy(email = newEmail)
      result <- dummyRepo.put(accountId, updated) match {
        case Some(_) => ZIO.succeed(updated)
        case None => ZIO.fail(AccountNotFoundById(accountId))
      }
    } yield result
  }

  override def getAccountByEmail(email: Email): IO[AccountNotFoundByEmail, Account] =
    dummyRepo.find(a => a._2.email == email) match {
      case Some((_, account)) => ZIO.succeed(account)
      case None => ZIO.fail(AccountNotFoundByEmail(email))
    }

  override def createAccount(email: Email, password: Password): IO[DuplicateAccount, Account] = {
    val accountId = AccountId.apply(RandomStringGenerator.randomAlphanumeric()).toOption.get

    addAccount(Account(accountId, email, password))
  }

  override def addAccount(newAccount: Account): IO[DuplicateAccount, Account] = {
    for {
      _ <- getAccountByEmail(newAccount.email).flip.orElseFail(DuplicateAccount(newAccount.email))
      result <-
        if dummyRepo.contains(newAccount.id) then ZIO.fail(DuplicateAccount(newAccount.email))
        else {
          dummyRepo += newAccount.id -> newAccount
          ZIO.succeed(newAccount)
        }
    } yield result
  }

  override def getAllAccounts: Task[List[Account]] = ZIO.attempt(dummyRepo.values.toList)
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