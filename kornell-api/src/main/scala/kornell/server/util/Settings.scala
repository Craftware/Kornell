package kornell.server.util

import java.util.{Date, Properties}

//Klangist Enums: https://gist.github.com/viktorklang/1057513
trait Enum {
  import java.util.concurrent.atomic.AtomicReference //Concurrency paranoia

  type EnumVal <: Value //This is a type that needs to be found in the implementing class

  private val _values = new AtomicReference(Vector[EnumVal]()) //Stores our enum values

  //Adds an EnumVal to our storage, uses CCAS to make sure it's thread safe, returns the ordinal
  private final def addEnumVal(newVal: EnumVal): Int = {
    import _values.{ get, compareAndSet => CAS }
    val oldVec = get
    val newVec = oldVec :+ newVal
    if ((get eq oldVec) && CAS(oldVec, newVec)) newVec.indexWhere(_ eq newVal) else addEnumVal(newVal)
  }

  def values: Vector[EnumVal] = _values.get //Here you can get all the enums that exist for this type

  //This is the trait that we need to extend our EnumVal type with, it does the book-keeping for us
  protected trait Value { self: EnumVal => //Enforce that no one mixes in Value in a non-EnumVal type
    final val ordinal = addEnumVal(this) //Adds the EnumVal and returns the ordinal

    def name: String //All enum values should have a name

    override def toString = name //And that name is used for the toString operation
    override def equals(other: Any) = this eq other.asInstanceOf[AnyRef]
    override def hashCode = 31 * (this.getClass.## + name.## + ordinal)
  }
}
//End of Klangist Magic

object Settings extends Enum {
  lazy val propsStream = Option(getClass.getResourceAsStream("/kornell.properties"))
  lazy val properties = propsStream map { stream =>
    val props = new Properties
    props.load(stream)
    props
  }

  sealed trait EnumVal extends Value {
    val default: Option[String] = None
    val required: Boolean = false

    lazy val getOpt = fromSystem
      .orElse(fromEnv)
      .orElse(fromProperties)
      .orElse(default)

    lazy val get = getOpt.getOrElse(null)
    lazy val fromSystem = Option(System.getProperty(name))
    lazy val fromEnv = Option(System.getenv(name))
    lazy val fromProperties: Option[String] = properties flatMap { props => Option(props.getProperty(name)) }

    lazy val requiredStr = if (required) "*" else " "
    lazy val valueStr =
      if (name.contains("PASSWORD")) "********"
      else if (getOpt.isDefined) s"= $get" else ""

    override def toString = s"[$requiredStr] $name $valueStr"
  }

  implicit def toString(e: EnumVal): String = e.get
  implicit def toOption(s: String): Option[String] = Option(s)

  def settting(_name: String) = new EnumVal {
    val name = _name
  }

  def settting(_name: String, _default: String) = new EnumVal {
    val name = _name
    override val default = Option(_default)
  }

  def settting(_name: String, _required: Boolean) = new EnumVal {
    val name = _name
    override val required = _required
  }

  val JDBC_CONNECTION_STRING = settting("JDBC_CONNECTION_STRING", "jdbc:mysql:///ebdb")
  val JDBC_USERNAME = settting("JDBC_USERNAME", "kornell")
  val JDBC_PASSWORD = settting("JDBC_PASSWORD")
  val JDBC_DRIVER = settting("JDBC_DRIVER", "com.mysql.jdbc.Driver")
  val TEST_MODE = settting("TEST_MODE")
  val SMTP_HOST = settting("SMTP_HOST")
  val SMTP_PORT = settting("SMTP_PORT")
  val SMTP_USERNAME = settting("SMTP_USERNAME")
  val SMTP_PASSWORD = settting("SMTP_PASSWORD")
  val SMTP_FROM = settting("SMTP_FROM", "suporte@eduvem.com")
  val BUILT_ON = settting("built.on")
  val DEFAULT_LOCALE = "pt_BR"
  val BUILD_NUM = {
    val buildNum = settting("build.number")
    if(buildNum.get != null && buildNum.get != "Development Build") buildNum else new EnumVal {
      val name = new Date().getTime.toString
    }
  }


  //??  
  def tmpDir =
    if (System.getProperty("java.io.tmpdir").endsWith("\\"))
      System.getProperty("java.io.tmpdir")
    else
      System.getProperty("java.io.tmpdir") + "/"
}