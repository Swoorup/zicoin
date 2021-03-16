package zicoin
package exception

import common.Hash
import proof.Proof

case class InvalidProofException(
    hash: Hash, 
    proof: Proof) extends Exception()
