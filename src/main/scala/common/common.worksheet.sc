import zicoin.common.*
import scala.language.strictEquality

object Foo extends NewIntType
type Foo = Foo.Type

object ABC extends NewIntType
type ABC = ABC.Type

Foo(12) == ABC(1)