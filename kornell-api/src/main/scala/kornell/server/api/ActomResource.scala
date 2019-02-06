package kornell.server.api

import java.sql.ResultSet
import java.util
import java.util.UUID

import javax.ws.rs._
import javax.ws.rs.core.Response
import kornell.core.entity.ActomEntries
import kornell.server.ep.EnrollmentSEP
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL._
import kornell.server.jdbc.repository.ActomEntriesRepo
import kornell.server.repository.Entities
import kornell.server.util.EnrollmentUtil._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class ActomResource(enrollmentUUID: String, actomURL: String) {
  implicit def toString(rs: ResultSet): String = rs.getString("entryValue")

  @GET
  def get(): String = actomKey

  val actomKey: String = if (actomURL.contains("?"))
    actomURL.substring(0, actomURL.indexOf("?"))
  else
    actomURL

  @Path("entries/{entryKey}")
  @GET
  def getValue(@PathParam("entryKey") entryKey: String): Response = {
    ActomEntriesRepo.getValue(enrollmentUUID, actomKey, entryKey)
    Response.noContent.build
  }

  @Path("entries/{entryKey}")
  @Consumes(Array("text/plain"))
  @PUT
  def putValue(@PathParam("entryKey") entryKey: String, entryValue: String): Response = {
    sql"""insert into ActomEntries (uuid, enrollmentUUID, actomKey, entryKey, entryValue)
    values (${UUID.randomUUID.toString}, ${enrollmentUUID} , ${actomKey}, ${entryKey}, ${entryValue})
    on duplicate key update entryValue = $entryValue
    """.executeUpdate
    Response.noContent.build
  }

  //Batch version of put value using a map
  def putValues(actomEntries: util.Map[String, String]): Int = {
    var queryModelQuery = "insert into ActomEntries (uuid, enrollmentUUID, actomKey, entryKey, entryValue) values "
    val queryModelStrings = new ListBuffer[String]
    for ((key, value) <- actomEntries) {
      queryModelStrings += ("('" + UUID.randomUUID.toString + "','" + enrollmentUUID + "','" + actomKey + "','" + key + "','" + value + "')")
    }
    queryModelQuery += queryModelStrings.mkString(",")
    queryModelQuery += " on duplicate key update entryValue = VALUES(entryValue)"

    new PreparedStmt(queryModelQuery, List[String]()).executeUpdate
  }

  @Path("entries")
  @Consumes(Array(ActomEntries.TYPE))
  @Produces(Array(ActomEntries.TYPE))
  @PUT
  def putEntries(entries: ActomEntries): ActomEntries = {
    val actomEntries = entries.getEntries

    val enrollmentMap = collection.mutable.Map[String, String]()
    for ((key, value) <- actomEntries)
      enrollmentMap(key) = value

    val enrollmentsJMap = enrollmentMap.asJava
    if (enrollmentsJMap.size() > 0) {
      putValues(enrollmentsJMap)
    }

    val hasProgress = containsProgress(actomEntries)
    if (hasProgress) {
      EnrollmentSEP.onProgress(enrollmentUUID)
    }
    val hasAssessment = containsAssessment(actomEntries)
    if (hasAssessment) {
      EnrollmentSEP.onAssessment(enrollmentUUID)
    }

    parsePreAssessmentScore(actomEntries)
      .foreach {
        EnrollmentSEP.onPreAssessmentScore(enrollmentUUID, _)
      }

    parsePostAssessmentScore(actomEntries)
      .foreach {
        EnrollmentSEP.onPostAssessmentScore(enrollmentUUID, _)
      }

    entries
  }

  @Path("entries")
  @Produces(Array(ActomEntries.TYPE))
  @GET
  def getEntries: ActomEntries = {
    val entries = Entities.newActomEntries(enrollmentUUID, actomKey, new util.HashMap[String, String])
    sql"""
    select * from ActomEntries
    where enrollmentUUID=${enrollmentUUID}
      and actomKey=${actomKey}""".foreach { rs =>
      entries.getEntries.put(rs.getString("entryKey"), rs.getString("entryValue"))
    }
    entries
  }

}

object ActomResource {
  def apply(enrollmentUUID: String, actomKey: String) = new ActomResource(enrollmentUUID, actomKey)
}
