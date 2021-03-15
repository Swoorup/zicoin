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

// Allow directly using this object+type without importing ProofOfWork
export ProofOfWork.Proof

object ProofOfWork:
  opaque type Proof = Long
  object Proof:
    def apply(a: Long): Proof = a
    extension (a: Proof) def value: Long = a

  def proofOfWork(lastHash: String): Proof = {
    @tailrec
    def powHelper(lastHash: String, proof: Proof): Proof = {
      if (validProof(lastHash, proof))
        proof
      else
        powHelper(lastHash, proof + 1)
    }

    val proof = 0
    powHelper(lastHash, proof)
  }

  /// test if the first 4 characters of the hash are zeroes
  def validProof(lastHash: String, proof: Proof): Boolean = {
    val guess = (lastHash ++ proof.toString).asJson.toString
    val guessHash = Crypto.sha256Hash(guess)
    (guessHash take 4) == "0000"
  }
