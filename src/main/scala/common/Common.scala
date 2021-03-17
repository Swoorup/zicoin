package zicoin
package common

import io.circe.{Codec, Decoder, Encoder, Json}

object Hash extends NewType[String]
type Hash = Hash.Type

type Timestamp = Long
