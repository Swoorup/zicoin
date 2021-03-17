package zicoin
package common

import scala.compiletime.erasedValue
import io.circe.{Encoder,Codec}

trait NewType[Wrapped]:
  opaque type Type = Wrapped
  def apply(w: Wrapped): Type = w
  extension (t: Type) def value: Wrapped = t
  given (using CanEqual[Wrapped, Wrapped]): CanEqual[Type, Type] = CanEqual.derived
  given (using ev: Encoder[Wrapped]): Encoder[Type] = ev
end NewType

/// Generics are boxed, this should improve perf
class NewIntType extends NewType[Int]:
  override opaque type Type = Int
  override def apply(a: Int): Type = a
  extension (a: Type) override def value: Int = a
end NewIntType

class NewLongType extends NewType[Long]:
  override opaque type Type = Long
  override def apply(a: Long): Type = a
  extension (a: Type) override def value: Long = a
end NewLongType
