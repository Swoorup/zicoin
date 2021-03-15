package zicoin
package exception

import blockchain.Hash
import proof.Proof

case class InvalidProofException(
    hash: Hash, 
    proof: Proof) extends Exception()
