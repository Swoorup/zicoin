import crypto.Crypto
import zicoin.blockchain.{*, given}
import io.circe.jawn.decode
import io.circe.syntax.*
import io.circe.parser.*
import zicoin.proof.ProofOfWork

val a = ChainLink(0, 0, Nil)
println(a)
println(a.hash)

val lastHash = a.hash
val proof = ProofOfWork.proofOfWork(lastHash)
val guess = (lastHash ++ proof.toString).asJson.toString
val guessHash = Crypto.sha256Hash(guess)