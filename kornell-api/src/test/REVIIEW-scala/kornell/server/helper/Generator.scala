package kornell.server.helper

import java.util.UUID

import scala.util.Random


trait Generator {
  Generator.init

  val names = List("Simon Iverson", "Scott Davis", "Paula Margot", "Susan Richards")
  val usernames = List("geek", "nerd", "gacto", "pri","sam")
  val domains = List("somedomain.co.uk", "fartmachine.com")

  def digits = Stream continually { Random.nextInt(9) }
  def chars = Stream continually { Random.nextPrintableChar }
  def randStr(length: Int): String = chars take length mkString
  def randStr: String = randStr(42)
  def randPassword = randStr(8)
  def randUUID: String = "[_test_]" + UUID.randomUUID.toString.substring(8)
  def randURL() = s"[_test_]https://${randStr}"
  def randDomain = randPick(domains)
  def randUsername = randPick(usernames) + digits.take(2).mkString
  def randEmail = s"${randUsername}@${randDomain}"
  def randName = randPick(names)
  def randInt(n: Int) = { Random.nextInt(n) }
  def randPick[T](l: Seq[T]): T = l(randInt(l.length))
  def randDigits(n: Int) = digits take n
  def randCPF = {    
    def addDigit(ds: Seq[Int]) = {
      val len = ds.length + 1
      assert(len == 10 || len == 11)
      val sum = ds.zipWithIndex
        .map { case (d, i) => d * (len - i) }
        .reduce { _ + _ }
      val mod = sum % 11
      val digit = if (mod > 1) 11 - mod else 0
      ds :+ digit
    }
    
    addDigit(addDigit(randDigits(9))) mkString
  }
}

object Generator {
  lazy val init = Random.setSeed(1337)    
  
}