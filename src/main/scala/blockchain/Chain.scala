package zicoin
package blockchain

import crypto.Crypto
import io.circe.generic.semiauto.*
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.syntax.*
import zicoin.proof.ProofOfWork.Proof

import java.security.InvalidParameterException

sealed trait Chain: //derives Codec.AsObject:
  val index: Int
  val hash: Hash
  val values: List[Transaction]
  val proof: Proof
  val timestamp: Timestamp

  def ::(link: Chain): Chain = link match
    case l:ChainLink => ChainLink(l.index, l.proof, l.values, this.hash, l.timestamp, this)
    case _ => throw new InvalidParameterException("Cannot add invalid link to chain")

object Chain:
  def apply[T](b: Chain*): Chain = {
    b match
      case Seq() => EmptyChain
      case Seq(l, xs @ _*) =>
        val link = l.asInstanceOf[ChainLink]
        ChainLink(link.index, link.proof, link.values, link.previousHash, link.timestamp, apply(xs:_*))
  }

// Also genesis block
case object EmptyChain extends Chain:
  val index: Int = 0
  val hash: Hash = "1"
  val values: List[Transaction] = Nil
  val proof: Proof = Proof(0)
  val timestamp: Timestamp = 0

case class ChainLink( index: Int,
                      proof: Proof,
                      values: List[Transaction],
                      previousHash: Hash = "",
                      timestamp: Timestamp = System.currentTimeMillis(),
                      tail: Chain = EmptyChain,
                    ) extends Chain:
  val hash: Hash = Crypto.sha256Hash(summon[Encoder[ChainLink]](this).asJson.toString)

given Encoder[Chain] = deriveEncoder[Chain]
given Encoder[ChainLink] = deriveEncoder[ChainLink]
