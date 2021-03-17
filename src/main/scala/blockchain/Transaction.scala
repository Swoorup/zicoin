package zicoin
package blockchain

import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.generic.semiauto.*
import io.circe.parser.*

case class Transaction( sender: Address, 
                        recipient: Address, 
                        value: Long) derives Encoder.AsObject