package kornell.server

import java.io.{File, FileInputStream, InputStream}
import java.util.{HashMap, UUID}

import kornell.server.jdbc.repository.{CourseClassRepo, CourseRepo}
import kornell.server.report.ReportCourseClassGenerator.getClass
import kornell.server.util.Settings
import net.sf.jasperreports.engine._
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import net.sf.jasperreports.engine.export.JRXlsExporter
import net.sf.jasperreports.engine.util.JRLoader

import scala.collection.JavaConverters.seqAsJavaListConverter

package object report {

  def getReportBytesFromStream(certificateData: List[Any], parameters: HashMap[String, Object], jasperStream: InputStream, fileType: String): Array[Byte] =
    runReportToPdf(certificateData, parameters, JRLoader.loadObject(jasperStream).asInstanceOf[JasperReport], fileType)

  def getReportBytesFromJrxml(certificateData: List[Any], parameters: HashMap[String, Object], jrxmlFileName: String, fileType: String): Array[Byte] = {
    val jrxmlFilePath = getClass.getResource("/reports/" + jrxmlFileName + ".jrxml").getPath
    val reportFile = new File(jrxmlFilePath)
    val inputStreamJR = new FileInputStream(reportFile)
    val reportCompiled = JasperCompileManager.compileReport(inputStreamJR)
    runReportToPdf(certificateData, parameters, reportCompiled, fileType)
  }

  def runReportToPdf(certificateData: List[Any], parameters: HashMap[String, Object], jasperReport: JasperReport, fileType: String): Array[Byte] =
    if (fileType != null && fileType == "xls") {
      val fileName = Settings.TMP_DIR + "tmp-" + UUID.randomUUID.toString + ".xls"
      val exporterXLS = new JRXlsExporter()
      val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(certificateData asJava))
      exporterXLS.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint)
      exporterXLS.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, fileName)
      exporterXLS.exportReport()
      val source = scala.io.Source.fromFile(fileName)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()
      byteArray
    } else
      JasperRunManager.runReportToPdf(jasperReport, parameters, new JRBeanCollectionDataSource(certificateData asJava))

  def clearJasperFiles: String = {
    val folder = new File(Settings.TMP_DIR)
    var deleted = "Deleting files from folder " + folder.getAbsolutePath + ": "
    folder.listFiles().foreach(file =>
      if (file.getName.endsWith(".jasper") || file.getName.endsWith(".xls") || file.getName.endsWith(".pdf")) {
        file.delete()
        deleted += file.getName + ", "
      })
    deleted
  }

  def getFileType(fileType: String): String = {
    if (fileType == "xls")
      "xls"
    else
      "pdf"
  }

  def getContentType(fileType: String): String = {
    if (getFileType(fileType) == "xls")
      "application/vnd.ms-excel"
    else
      "application/pdf"
  }

  def getInstitutionUUID(courseClassUUID: String, courseUUID: String = null): String = {
    if (courseUUID != null) {
      CourseRepo(courseUUID).get.getInstitutionUUID
    } else {
      CourseClassRepo(courseClassUUID).get.getInstitutionUUID
    }
  }
}
