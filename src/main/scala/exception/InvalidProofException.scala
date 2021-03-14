package zicoin
package exception

import blockchain.Hash

case class InvalidProofException(
    hash: Hash, 
    proof: Long) extends Exception()
