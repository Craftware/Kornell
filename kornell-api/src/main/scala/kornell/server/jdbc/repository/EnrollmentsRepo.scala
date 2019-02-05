package kornell.server.jdbc.repository

import java.util.{Date, UUID}
import java.util.concurrent.TimeUnit.MINUTES

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import kornell.core.entity._
import kornell.core.error.exception.{EntityConflictException, ServerErrorException}
import kornell.core.to._
import kornell.server.api.getAuthenticatedPersonUUID
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL._
import kornell.server.repository.{Entities, TOs}
import org.joda.time.DateTime

import scala.collection.JavaConverters._

object EnrollmentsRepo {

  def byCourseClass(courseClassUUID: String): EnrollmentsTO =
    byCourseClassPaged(courseClassUUID, "", Int.MaxValue, 1, "e.state", false)

  def byCourseClassPaged(courseClassUUID: String, searchTerm: String, pageSize: Int, pageNumber: Int, orderBy: String, asc: Boolean, bindMode: String = null): EnrollmentsTO = {
    val resultOffset = (pageNumber.max(1) - 1) * pageSize
    val filteredSearchTerm = '%' + Option(searchTerm).getOrElse("") + '%'
    val orderColumn = if (orderBy != null && !orderBy.contains(";")) orderBy else "e.state"
    val orderMod = if (asc) " asc" else " desc"
    val order = orderColumn + orderMod + (if (orderColumn.contains("e.progress")) ", e.assessmentScore " + orderMod else "")

    val enrollmentsTO = TOs.newEnrollmentsTO(
      new PreparedStmt(s"""
        select
          e.*,
          p.uuid as personUUID,
          p.fullName,
          if(pw.username is not null, pw.username, p.email) as username
        from Enrollment e
        join Person p on e.personUUID = p.uuid
        left join Password pw on p.uuid = pw.personUUID
        where e.courseClassUUID = '${courseClassUUID}' and
             e.state <> '${EnrollmentState.deleted.toString}' and
                (p.fullName like '${filteredSearchTerm}'
                or pw.username like '${filteredSearchTerm}'
                or p.email like '${filteredSearchTerm}')
        order by ${order}, e.state desc, p.fullName limit ${resultOffset}, ${pageSize}
      """, List[String]()).map[EnrollmentTO](toEnrollmentTO))
    enrollmentsTO.setCount(countByCourseClass(courseClassUUID))
    enrollmentsTO.setCountCancelled(countByCourseClassAndState(courseClassUUID, EnrollmentState.cancelled))
    enrollmentsTO.setPageSize(pageSize)
    enrollmentsTO.setPageNumber(pageNumber.max(1))
    enrollmentsTO.setSearchCount({
      if (searchTerm == "")
        0
      else
        sql"""
          select count(*) from (
            select if(pw.username is not null, pw.username, p.email) as username
            from Enrollment e
            join Person p on e.personUUID = p.uuid
            left join Password pw on p.uuid = pw.personUUID
            where e.courseClassUUID = ${courseClassUUID} and
            e.state <> ${EnrollmentState.deleted.toString} and
            (p.fullName like ${filteredSearchTerm}
            or pw.username like ${filteredSearchTerm}
            or p.email like ${filteredSearchTerm})
           ) as a
        """.first[String].get.toInt
    })

    if("hasPowerOver" == bindMode) {
      val personRepo = PersonRepo(getAuthenticatedPersonUUID)
      enrollmentsTO.getEnrollmentTOs.asScala.foreach(enrollmentTO => {
        enrollmentTO.setHasPowerOver(personRepo.hasPowerOver(enrollmentTO.getPersonUUID))
        enrollmentsTO.isInstanceOf
      })
    }
    enrollmentsTO
  }

  def countByCourseClass(courseClassUUID: String): Int =
    sql"""select count(*)
      from Enrollment e
      where e.courseClassUUID = ${courseClassUUID}
      and e.state <> ${EnrollmentState.deleted.toString}
    """.first[String].get.toInt

  def countByCourseClassAndState(courseClassUUID: String, enrollmentState: EnrollmentState): Int =
    sql"""select count(*)
      from Enrollment e
      where e.courseClassUUID = ${courseClassUUID}
      and e.state = ${enrollmentState.toString}
    """.first[String].get.toInt

  def byPerson(personUUID: String): List[Enrollment] =
    sql"""
      SELECT
      e.*
      FROM Enrollment e join Person p on e.personUUID = p.uuid
      WHERE e.personUUID = ${personUUID} and e.state <> ${EnrollmentState.deleted.toString}
    """.map[Enrollment](toEnrollment)

  def byCourseClassAndPerson(courseClassUUID: String, personUUID: String, getDeleted: Boolean): Option[Enrollment] =
    sql"""
      SELECT e.*, p.*
      FROM Enrollment e join Person p on e.personUUID = p.uuid
      WHERE e.courseClassUUID = ${courseClassUUID} and
        (e.state <> ${EnrollmentState.deleted.toString} or ${getDeleted} = true)
        AND e.personUUID = ${personUUID}
    """.first[Enrollment]

  def byCourseClassAndUsername(courseClassUUID: String, username: String): Option[String] =
    sql"""
      SELECT e.uuid
      FROM Enrollment e join Person p on e.personUUID = p.uuid
      join Password pw on pw.personUUID = p.uuid
      WHERE e.courseClassUUID = ${courseClassUUID} and e.state <> ${EnrollmentState.deleted.toString}
      AND pw.username = ${username}
    """.first[String]

  def byCourseVersionAndPerson(courseVersionUUID: String, personUUID: String): Option[Enrollment] =
    sql"""
      SELECT e.*, p.*
      FROM Enrollment e join Person p on e.personUUID = p.uuid
      WHERE e.courseVersionUUID = ${courseVersionUUID} and e.state <> ${EnrollmentState.deleted.toString}
      AND e.personUUID = ${personUUID}
    """.first[Enrollment]

  def byStateAndPerson(state: EnrollmentState, personUUID: String): List[EnrollmentTO] =
    sql"""
      SELECT e.*, p.*
      FROM Enrollment e join Person p on e.personUUID = p.uuid
      WHERE e.personUUID = ${personUUID}
      AND e.state = ${state.toString}
      ORDER BY e.state desc, p.fullName, p.email
    """.map[EnrollmentTO](toEnrollmentTO)

  def create(
    courseClassUUID: String,
    personUUID: String,
    enrollmentState: EnrollmentState,
    courseVersionUUID: String,
    parentEnrollmentUUID: String,
    enrollmentSource: EnrollmentSource): Enrollment =
    create(Entities.newEnrollment(null, null, courseClassUUID, personUUID, null, "", enrollmentState, null, null, null, null, null, courseVersionUUID, parentEnrollmentUUID, null, null, null, null, enrollmentSource))

  def create(enrollment: Enrollment): Enrollment = {

    if (enrollment.getUUID == null)
      enrollment.setUUID(UUID.randomUUID.toString)
    if (enrollment.getCourseClassUUID != null && enrollment.getCourseVersionUUID != null)
      throw new EntityConflictException("doubleEnrollmentCriteria")

    val endDate: Date = if (enrollment.getEndDate != null){
      enrollment.getEndDate
    } else if (enrollment.getCourseClassUUID != null) {
      val courseClass = CourseClassRepo(enrollment.getCourseClassUUID).get
      val enrollmentExpiryDays = courseClass.getEnrollmentExpiryDays
      if (enrollmentExpiryDays == null || enrollmentExpiryDays <= 0) null else {
        DateTime.now.plusDays(enrollmentExpiryDays).toDate
      }
    } else null

    sql"""
      insert into Enrollment(uuid,courseClassUUID,personUUID,enrolledOn,state,courseVersionUUID,parentEnrollmentUUID,enrollmentSource,endDate)
      values(
        ${enrollment.getUUID},
        ${enrollment.getCourseClassUUID},
        ${enrollment.getPersonUUID},
        now(),
        ${enrollment.getState.toString},
        ${enrollment.getCourseVersionUUID},
        ${enrollment.getParentEnrollmentUUID},
        ${enrollment.getEnrollmentSource.toString},
        ${endDate}
      )""".executeUpdate
    if (enrollment.getCourseClassUUID != null)
      ChatThreadsRepo.addParticipantsToCourseClassThread(CourseClassesRepo(enrollment.getCourseClassUUID).get)
    enrollment
  }

  def find(personUUID: String, courseClassUUID: String): Option[Enrollment] = sql"""
    select * from Enrollment
    where personUUID=${personUUID}
     and courseClassUUID=${courseClassUUID}"""
    .first[Enrollment]

  def simplePersonList(courseClassUUID: String): SimplePeopleTO = {
    TOs.newSimplePeopleTO(sql"""select p.uuid as uuid, p.fullName as fullName, pw.username as username
    from Enrollment enr
    join Person p on enr.personUUID = p.uuid
    left join Password pw on p.uuid = pw.personUUID
    where enr.state <> ${EnrollmentState.cancelled.toString} and enr.state <> ${EnrollmentState.deleted.toString} and
    enr.courseClassUUID = ${courseClassUUID}""".map[SimplePersonTO](toSimplePersonTO))
  }

  def getEspinafreEmailList: List[Person] = {
    sql"""select p.* from Enrollment e
        join CourseVersion cv on cv.uuid = e.courseVersionUUID
      join Person p on e.personUUID = p.uuid
        where cv.label = 'espinafre'
      and p.receiveEmailCommunication = 1
      and e.endDate = concat(curdate(), ' 23:59:59')
      and e.progress < 100
    """.map[Person](toPerson)
  }

  def getLeaderboardForDashboard(dashboardEnrollmentUUID: String): DashboardLeaderboardTO = {
    val courseClass = CourseClassesRepo.byEnrollment(dashboardEnrollmentUUID)
    if (courseClass.isEmpty) throw new ServerErrorException("errorGeneratingReport")
    val institution = InstitutionRepo(courseClass.get.getInstitutionUUID).get
    if (!InstitutionType.DASHBOARD.equals(institution.getInstitutionType)) throw new ServerErrorException("errorGeneratingReport")
    TOs.newDashboardLeaderboardTO(
      sql"""
        select
          p.uuid,
          p.fullName,
          (select ae.entryValue from ActomEntries ae where ae.enrollmentUUID = e.uuid and ae.entryKey = "knl.leaderboardScore") as attribute,
          CONVERT(SUBSTRING_INDEX((select attribute),'-',-1),UNSIGNED INTEGER) as clean_attribute
        from Person p
          join Enrollment e on e.personUUID = p.uuid
        where
            e.uuid in (select uuid from Enrollment where courseClassUUID in (select courseClassUUID from Enrollment where uuid = ${dashboardEnrollmentUUID}))
        order by clean_attribute desc, p.fullName;
        """.map[DashboardLeaderboardItemTO](toDashboardLeaderboardItemTO))
  }

  def getLeaderboardPosition(dashboardEnrollmentUUID: String): String = {
    val personUUID = sql" SELECT personUUID FROM Enrollment e WHERE uuid = ${dashboardEnrollmentUUID}".first[String].get
    var personAttribute = 0
    val leaderboardItems = getLeaderboardForDashboard(dashboardEnrollmentUUID).getDashboardLeaderboardItems
    for (i <- 0 until leaderboardItems.size; if personAttribute == 0) {
      val item = leaderboardItems.get(i)
      if (item.getPersonUUID.equals(personUUID))
        personAttribute = item.getAttribute.toInt
    }
    (leaderboardItems.asScala.count(_.getAttribute.toInt > personAttribute) + 1).toString
  }

  val cacheBuilder: CacheBuilder[AnyRef, AnyRef] = CacheBuilder
    .newBuilder()
    .expireAfterAccess(5, MINUTES)
    .maximumSize(1000)

  val uuidLoader: CacheLoader[String, Option[Enrollment]] = new CacheLoader[String, Option[Enrollment]]() {
    override def load(uuid: String): Option[Enrollment] = sql" SELECT * FROM Enrollment e WHERE uuid = ${uuid}".first[Enrollment]
  }

  val uuidCache: LoadingCache[String, Option[Enrollment]] = cacheBuilder.build(uuidLoader)

  def getByUUID(uuid: String): Option[Enrollment] = Option(uuid) flatMap uuidCache.get

  def updateCache(e: Enrollment): Unit = {
    val oe = Some(e)
    uuidCache.put(e.getUUID, oe)
  }

  def invalidateCache(enrollmentUUID: String): Unit = {
    uuidCache.invalidate(enrollmentUUID)
  }

  def clearCache(): Unit = uuidCache.invalidateAll()

}
