package com.virtuslab.akkaworkshop

import org.apache.commons.codec.binary.Base64

import scala.util.Random

sealed trait DecryptionState

case class PasswordPrepared(password: String) extends DecryptionState

case class PasswordDecoded(password: String) extends DecryptionState

object Decrypter {

  private val maxClientsCount = 4

  private val random = new Random()

  private var clientsCount = 0

  private var clients = Set.empty[Int]

  private var currentId = 0

  private def randomAlphanumericString(n: Int): String =
    random.alphanumeric.take(n).mkString

  private def getNewId: Int = {
    this synchronized {
      val id = currentId
      clients = clients + id
      currentId += 1
      id
    }
  }

  private def isClientAccepted = synchronized {
    if (clientsCount < maxClientsCount) {
      clientsCount += 1
      true
    } else {
      false
    }
  }

  private def message =
    """Internal state of decryptor get corrupted.
      |This is not your fault - this is intended "problem" :)
      |Beware that all current decryptor instances also get corrupted and will produce bad results.
      |In order to correctly use decryptor library please create new instances.
    """.stripMargin

  private def decrypt(id: Int, password: String, probabilityOfFailure: Double = 0.05) = {
    try {
      Thread.sleep(1000)

      while (!isClientAccepted) {
        Thread.sleep(100)
      }

      this synchronized {
        val shouldFail = random.nextDouble() < probabilityOfFailure
        if (shouldFail) {
          clients = clients.empty
          throw new IllegalStateException(message)
        }
        if (clients.contains(id))
          new String(Base64.decodeBase64(password.getBytes))
        else
          randomAlphanumericString(20)
      }
    } finally {
      synchronized {
        clientsCount -= 1
      }
    }
  }
}

class Decrypter {
  val id = Decrypter.getNewId

  def prepare(password: String): PasswordPrepared =
    PasswordPrepared(Decrypter.decrypt(id, password))

  def decode(state: PasswordPrepared): PasswordDecoded = PasswordDecoded(Decrypter.decrypt(id, state.password))

  def decrypt(state: PasswordDecoded): String = Decrypter.decrypt(id, state.password)
}
