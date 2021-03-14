package zicoin
package proof

import io.circe
import io.circe.generic.semiauto.*
import io.circe.syntax
import io.circe.jawn.decode
import io.circe.syntax.*
import io.circe.parser.*
import crypto.Crypto
import scala.annotation.tailrec

object ProofOfWork:

  def proofOfWork(lastHash: String): Long = {
    @tailrec
    def powHelper(lastHash: String, proof: Long): Long = {
      if (validProof(lastHash, proof))
        proof
      else
        powHelper(lastHash, proof + 1)
    }

    val proof = 0
    powHelper(lastHash, proof)
  }

  /// test if the first 4 characters of the hash are zeroes
  def validProof(lastHash: String, proof: Long): Boolean = {
    val guess = (lastHash ++ proof.toString).asJson.toString
    val guessHash = Crypto.sha256Hash(guess)
    (guessHash take 4) == "0000"
  }
