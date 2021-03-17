package zicoin
package blockchain

import common.NewType

type Address = Address.Type
object Address extends NewType[String]:
  def CoinBase: Address = Address("Coinbase")