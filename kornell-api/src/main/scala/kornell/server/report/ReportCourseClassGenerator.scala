package kornell.server.report

import java.io.InputStream
import java.sql.ResultSet
import java.util.Date
import java.util.HashMap
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.ListBuffer
import kornell.core.to.report.CourseClassReportTO
import kornell.core.to.report.EnrollmentsBreakdownTO
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.repository.TOs
import kornell.core.entity.EnrollmentState
import kornell.core.entity.CourseClassState
import kornell.core.util.StringUtils._
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL.SQLHelper

object ReportCourseClassGenerator {

  implicit def toCourseClassReportTO(rs: ResultSet): CourseClassReportTO =
    TOs.newCourseClassReportTO(
		rs.getString("fullName"),
		rs.getString("username"),
		rs.getString("email"),
		rs.getString("cpf"),
		rs.getString("state"),
		rs.getString("progressState"),
		rs.getInt("progress"),
		rs.getBigDecimal("assessmentScore"),
		rs.getTimestamp("certifiedAt"),
		rs.getTimestamp("enrolledAt"),
		rs.getString("courseName"),
		rs.getString("courseVersionName"),
		rs.getString("courseClassName"),
		rs.getString("company"),
		rs.getString("title"),
		rs.getString("sex"),
		rs.getString("birthDate"),
		rs.getString("telephone"),
		rs.getString("country"),
		rs.getString("stateProvince"),
		rs.getString("city"),
		rs.getString("addressLine1"),
		rs.getString("addressLine2"),
		rs.getString("postalCode"))
      
  type BreakdownData = Tuple2[String,Integer] 
  implicit def breakdownConvertion(rs:ResultSet): BreakdownData = (rs.getString(1), rs.getInt(2))
  
  def generateCourseClassReport(courseUUID: String, courseClassUUID: String, fileType: String): Array[Byte] = {
    val courseClassReportTO = sql"""
			select 
				p.fullName, 
				if(pw.username is not null, pw.username, p.email) as username,
				p.email,
    			p.cpf,
				case    
					when e.state = ${EnrollmentState.cancelled.toString} then 'Cancelada'  
					when e.state = ${EnrollmentState.requested.toString} then 'Requisitada'  
					when e.state = ${EnrollmentState.denied.toString} then 'Negada'  
					else 'Matriculado'   
				end as state,
				case    
					when progress is null OR progress = 0 then 'notStarted'  
					when progress > 0 and progress < 100 then 'inProgress'  
					when progress = 100 and certifiedAt is null then 'waitingEvaluation'  
					else 'completed'   
				end as progressState,
				e.progress,
				e.assessmentScore,
				e.certifiedAt,
				e.enrolledOn as enrolledAt,
    			c.title as courseName,
    			cv.name as courseVersionName,
    			cc.name as courseClassName,
				p.company,
				p.title,
				p.sex,
				p.birthDate,
				p.telephone,
				p.country,
				p.state as stateProvince,
				p.city,
				p.addressLine1,
				p.addressLine2,
				p.postalCode
			from 
				Enrollment e 
				join Person p on p.uuid = e.person_uuid
				join CourseClass cc on cc.uuid = e.class_uuid
				join CourseVersion cv on cv.uuid = cc.courseVersion_uuid
				join Course c on c.uuid = cv.course_uuid
				left join Password pw on pw.person_uuid = p.uuid
			where
				(e.state = ${EnrollmentState.enrolled.toString} or ${fileType} = 'xls') and
    			cc.state <> ${CourseClassState.deleted.toString} and 
    			(cc.state = ${CourseClassState.active.toString} or ${courseUUID} is null) and
		  		(e.class_uuid = ${courseClassUUID} or ${courseClassUUID} is null) and
				(c.uuid = ${courseUUID} or ${courseUUID} is null) and
				e.state <> ${EnrollmentState.deleted.toString}
			order by 
				case 
					when e.state = ${EnrollmentState.enrolled.toString} then 1
					when e.state = ${EnrollmentState.requested.toString}  then 2
					when e.state = ${EnrollmentState.denied.toString}  then 3
					when e.state = ${EnrollmentState.cancelled.toString}  then 4
					else 5
					end,
				case 
					when progressState = 'completed' then 1
					when progressState = 'waitingEvaluation'  then 2
					when progressState = 'inProgress'  then 3
					else 4 
					end,
				c.title,
				cv.name,
				cc.name,
				e.certifiedAt,
				progress,
				p.fullName,
				pw.username,
				p.email
	    """.map[CourseClassReportTO](toCourseClassReportTO)
	    
	    val parameters = getTotalsAsParameters(courseUUID, courseClassUUID, fileType)
	    addInfoParameters(courseUUID, courseClassUUID, parameters)
	
	    val enrollmentBreakdowns: ListBuffer[EnrollmentsBreakdownTO] = ListBuffer()
	    enrollmentBreakdowns += TOs.newEnrollmentsBreakdownTO("aa", new Integer(1))
	    enrollmentBreakdowns.toList
		  
	    val cl = Thread.currentThread.getContextClassLoader
	    val jasperStream = {
      	if(fileType != null && fileType == "xls")
      	  cl.getResourceAsStream("reports/courseClassInfoXLS.jasper")
      	else
      	  cl.getResourceAsStream("reports/courseClassInfo.jasper")
    	}
	    ReportGenerator.getReportBytesFromStream(courseClassReportTO, parameters, jasperStream, fileType)
  }
      
  type ReportHeaderData = Tuple9[String,String, String, Date, String, String, String, String, String]
  implicit def headerDataConvertion(rs:ResultSet): ReportHeaderData = (rs.getString(1), rs.getString(2), rs.getString(3), rs.getDate(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9))
  
  private def addInfoParameters(courseUUID: String, courseClassUUID: String, parameters: HashMap[String, Object]) = {
    val headerInfo = sql"""
			select 
				i.fullName as 'institutionName',
				c.title as 'courseTitle',
				cc.name as 'courseClassName',
				cc.createdAt,
				cc.maxEnrollments,
				i.assetsRepositoryUUID,
				(select eventFiredAt from CourseClassStateChanged 
					where toState = 'inactive' and courseClassUUID = cc.uuid
					order by eventFiredAt desc) as disabledAt,
				(select replace(GROUP_CONCAT(p.fullName),',',', ')
					from Role r 
					join Person p on p.uuid = r.person_uuid 
					where course_class_uuid = cc.uuid
					group by course_class_uuid) as courseClassAdminNames,
                    i.baseURL
			from
				CourseClass cc
				join CourseVersion cv on cc.courseVersion_uuid = cv.uuid
				join Course c on cv.course_uuid = c.uuid
				join Institution i on i.uuid = cc.institution_uuid
			where (cc.uuid = ${courseClassUUID} or ${courseClassUUID} is null) and
				(cv.course_uuid = ${courseUUID} or ${courseUUID} is null) and
    			cc.state <> ${CourseClassState.deleted.toString} and 
    			(cc.state = ${CourseClassState.active.toString} or ${courseUUID} is null)
    """.first[ReportHeaderData](headerDataConvertion)
    
    parameters.put("institutionName", headerInfo.get._1)
    parameters.put("courseTitle", headerInfo.get._2)
	parameters.put("assetsURL", mkurl(headerInfo.get._9, "repository", headerInfo.get._6, ""))
    println(parameters.get("assetsURL"))
    if(courseClassUUID != null){
	    parameters.put("courseClassName", headerInfo.get._3)
	    parameters.put("createdAt", headerInfo.get._4)
	    parameters.put("maxEnrollments", headerInfo.get._5)
	    parameters.put("disabledAt", headerInfo.get._7)
	    parameters.put("courseClassAdminNames", headerInfo.get._8)
    }
    
    parameters
  }

  private def getTotalsAsParameters(courseUUID: String, courseClassUUID: String, fileType: String): HashMap[String,Object] = {
    val enrollmentStateBreakdown = sql"""
    		select 
					case    
						when progress is null OR progress = 0 then 'notStarted'  
						when progress > 0 and progress < 100 then 'inProgress'  
						when progress = 100 and certifiedAt is null then 'waitingEvaluation'  
						else 'completed'   
					end as progressState,
					count(*) as total
				from 
					Enrollment e 
					join CourseClass cc on cc.uuid = e.class_uuid
					join CourseVersion cv on cv.uuid = cc.courseVersion_uuid
				where      
					(e.state = ${EnrollmentState.enrolled.toString} or ${fileType} = 'xls') and
    				cc.state = ${CourseClassState.active.toString} and 
    		  		(e.class_uuid = ${courseClassUUID} or ${courseClassUUID} is null) and
					(cv.course_uuid = ${courseUUID} or ${courseUUID} is null) and
					e.state <> ${EnrollmentState.deleted.toString}
				group by 
					case    
						when progress is null OR progress = 0 then 'notStarted'  
						when progress > 0 and progress < 100 then 'inProgress' 
						when progress = 100 and certifiedAt is null then 'waitingEvaluation'  
						else 'completed'   
					end
		    """.map[BreakdownData](breakdownConvertion)
		    
    val parameters: HashMap[String, Object] = new HashMap()
    enrollmentStateBreakdown.foreach(rd => parameters.put(rd._1, rd._2)) 
    parameters
  }

 
}