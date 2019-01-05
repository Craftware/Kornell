package kornell.server.web

import java.util.logging.Logger

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import kornell.server.util.DateConverter

class InstitutionHostnameFilter extends Filter {

  val logger: Logger = Logger.getLogger("kornell.server.web")
  val DOMAIN_HEADER = "X-KNL-DOMAIN"

  override def doFilter(sreq: ServletRequest, sres: ServletResponse, chain: FilterChain): Unit =
    (sreq, sres) match {
      case (hreq: HttpServletRequest, hres: HttpServletResponse) => {
        if (hreq.getRequestURI.startsWith("/api") && !hreq.getRequestURI.equals("/api") && !hreq.getRequestURI.equals("/api/")) {
          doFilter(hreq, hres, chain)
        } else {
          chain.doFilter(hreq, hres)
        }
      }
    }

  def doFilter(req: HttpServletRequest, resp: HttpServletResponse, chain: FilterChain): Unit = {
    //    debugLogRequest(req)
    val institution = FilterUtils.getInstitution(req, DOMAIN_HEADER)

    if (institution.isDefined) {
      DateConverter.setTimeZone(institution.get.getTimeZone)
      chain.doFilter(req, resp)
      clearTimeZone()
    } else {
      logAndContinue(req, resp, chain)
    }
  }

  def logAndContinue(req: HttpServletRequest, resp: HttpServletResponse, chain: FilterChain): Unit = {
    logger.warning("Request did not contain a valid 'X-KNL-DOMAIN' header, could not initialize DateConverter for URL " + req.getRequestURL)
    chain.doFilter(req, resp)
  }

  def debugLogRequest(request: HttpServletRequest): Unit = {
    val pathString = {
      if (request.getQueryString != null) request.getMethod + " " + request.getRequestURI + "?" + request.getQueryString
      else request.getMethod + " " + request.getRequestURI
    }
    logger.info(pathString)
    val headers = request.getHeaderNames
    while (headers.hasMoreElements) {
      val header = headers.nextElement
      logger.info(header + ": " + request.getHeader(header))
    }
  }

  override def init(cfg: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  def clearTimeZone(): Unit = DateConverter.clearTimeZone()
}