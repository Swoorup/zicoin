package zicoin
package exception

import common.Hash

case class MinerBusyException(msg: String) extends Exception(msg)