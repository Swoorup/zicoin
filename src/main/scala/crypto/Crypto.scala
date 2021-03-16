package zicoin
package crypto

import java.math.BigInteger
import java.security.MessageDigest

object Crypto:
  def sha256Hash(value: String): String = { 
    val digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"))
    String.format("%064x", new BigInteger(1, digest))
  }
