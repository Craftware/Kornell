package kornell.server.jdbc

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.util.{Date => JDate}
import java.math.BigDecimal
import java.sql.SQLException
import java.util.logging.Level

import kornell.core.error.exception.EntityConflictException

import scala.collection.mutable.ListBuffer

class PreparedStmt(query: String, params: List[Any]) {

  def connected[T](fun: Connection => T): T = {
    val conn = ConnectionHandler.getConnection
    fun(conn)
  }

  def prepared[T](fun: PreparedStatement => T): T =
    connected { conn =>
      val pstmt = conn.prepareStatement(query.stripMargin.trim)

      def setQueryParam(i: Int, param: Any): Unit = try {
        param match {
          case null => pstmt.setObject(i, null)
          case p: String => pstmt.setString(i, p)
          case p: Integer => pstmt.setInt(i, p)
          case p: Double => pstmt.setDouble(i, p)
          case p: JDate => pstmt.setTimestamp(i, new Timestamp(p.getTime))
          //          case p: KDate => pstmt.setDate(i, new SQLDate(TimeUtil.toJUD(p).getTime()))
          case p: BigDecimal => pstmt.setBigDecimal(i, p)
          case p: Boolean => pstmt.setBoolean(i, p)
          //TODO: make this work: case (p: Entity, i) => pstmt.setString(i, p.getUUID)
          case _ => throw new EntityConflictException("invalidValue")
        }
      } catch {
        case e: SQLException => {
          logger.log(Level.SEVERE, "SQLException for setting param: ", e)
          logger.severe(s"Failed to set param [${i}] value to [${param}] on query ${query}")
        }
      }

      params
        .zipWithIndex
        .foreach(param => setQueryParam(param._2 + 1, param._1))

      logger.finer(s"Executing query [$query].")

      try fun(pstmt)
      finally pstmt.close()
    }

  def executeUpdate: Int = prepared { stmt => stmt.executeUpdate }

  def executeQuery[T](fun: ResultSet => T): T = prepared { stmt =>
    val rs = stmt.executeQuery
    try fun(rs)
    finally rs.close()
  }

  def executeQuery(): Unit = executeQuery { _ => () }

  def foreach(fun: ResultSet => Unit): Unit = executeQuery { rs =>
    while (rs.next) fun(rs)
  }

  //TODO: Consider converting to a Stream for lazy processing
  def map[T](implicit fun: ResultSet => T): List[T] = {
    val xs: ListBuffer[T] = ListBuffer()
    foreach { rs => xs += fun(rs) }
    xs.toList
  }

  def first[T](implicit conversion: ResultSet => T): Option[T] = prepared { stmt =>
    stmt.setMaxRows(1)
    val rs = stmt.executeQuery
    if (!rs.next) None
    else
      try Option(conversion(rs))
      finally rs.close()
  }

  def isPositive: Boolean = executeQuery { rs =>
    rs.next && rs.getInt(1) > 0
  }

  def getUUID: String = executeQuery { rs =>
    if (rs.next) rs.getString("uuid")
    else throw new IllegalStateException("Can not get on empty result")
  }

  def get[T](implicit conversion: ResultSet => T): T = first(conversion).get

  override def toString: String = query
}
