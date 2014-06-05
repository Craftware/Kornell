package kornell.server.jdbc

import java.sql.Connection
import kornell.core.util.UUID
import java.sql.ResultSet

object SQL {
  implicit def rsToString(rs:ResultSet):String = rs.getString(1);


  type ConnectionFactory = () => Connection

  implicit class SQLHelper(val sc: StringContext) extends AnyVal {
    def sql(args: Any*) = {
      val parts = sc.parts.iterator
      val params = args.toList
      var query = new StringBuffer(parts.next)
      while (parts.hasNext) {
        query append "?"
        query append parts.next
      }
      new PreparedStmt(query.toString, params)
    }
  }

  def randomUUID = UUID.randomUUID()
}