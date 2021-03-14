package zicoin
package exception

import blockchain.Hash

case class MinerBusyException(msg: String) extends Exception(msg)