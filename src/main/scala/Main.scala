import zicoin.crypto.Crypto
import play.api.libs.json.*
import play.api.libs.functional.syntax.*

@main def hello: Unit = {
    println("Hello world!")
    println(msg)
}

def msg = "I was compiled by Scala 3. :)"