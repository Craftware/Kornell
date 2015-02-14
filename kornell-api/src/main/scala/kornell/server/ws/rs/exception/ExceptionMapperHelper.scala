package kornell.server.ws.rs.exception

import com.google.web.bindery.autobean.vm.AutoBeanFactorySource

import javax.ws.rs.core.Response
import kornell.core.error.KornellErrorTO
import kornell.core.to.TOFactory

class ExceptionMapperHelper {

}

object ExceptionMapperHelper {
  val errorFactory = AutoBeanFactorySource.create(classOf[TOFactory])
  
  def handleError(code: Int, messageKey: String, exception: String = null): Response = {
    var errorTO = errorFactory.newErrorTO.as;
    errorTO.setMessageKey(messageKey)
    errorTO.setException(exception)
    Response
    	.status(code)
    	.entity(errorTO)
    	.header("Content-Type", KornellErrorTO.TYPE)
    	.build()
  }
}