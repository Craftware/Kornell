package db.jdbcmigration

import com.googlecode.flyway.core.api.migration.jdbc.JdbcMigration
import java.sql.Connection
import kornell.server.util.EnrollmentUtil._
import kornell.server.jdbc.repository.EnrollmentRepo
import kornell.server.jdbc.SQL._
import kornell.server.jdbc.ConnectionHandler

class V2016_02_13_15_02__PreAssessmentScore extends JdbcMigration  {
  override def migrate(conn: Connection) {
    migratePreAssessmentScores
  }

  def migratePreAssessmentScores() = {
    sql"""
		  select ae.enrollment_uuid,ae.entryValue 
		  from ActomEntries ae
		  join Enrollment e on ae.enrollment_uuid = e.uuid
		  where entryKey = 'cmi.suspend_data' 
				and ae.entryValue like '%preteste%'
				and e.preAssessmentScore is null
		""".foreach { rs =>
      val uuid = rs.getString("enrollment_uuid")
      val sdata = rs.getString("entryValue")
      for (score <- parsePreScore(sdata)) {
        println(s"Updating [${uuid}] preAssessment [${score}]")
        EnrollmentRepo(uuid).updatePreAssessmentScore(score)
      }
    }
    ConnectionHandler.commit()
  }
}
