package kornell.server.api

import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.POST
import kornell.server.jdbc.repository.AuthRepo
import kornell.core.entity.AuthClientType
import org.joda.time.DateTime
import java.util.Date
import kornell.core.util.UUID
import kornell.server.jdbc.repository.TokenRepo
import javax.ws.rs.QueryParam
import javax.ws.rs.Produces
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import kornell.core.to.TokenTO
import kornell.core.error.exception.UnauthorizedAccessException
import javax.ws.rs.core.Context
import javax.servlet.http.HttpServletRequest
import kornell.core.error.exception.AuthenticationException
import javax.ws.rs.core.Response

@Path("auth")
class TokenResource {

  @POST
  @Produces(Array(TokenTO.TYPE))
  @Path("token")
  def getToken(@FormParam("clientType") clientType: String, @FormParam("institutionUUID") institutionUUID: String,
    @FormParam("userkey") userkey: String, @FormParam("password") password: String) = {
    //gotta escape because form params and plus signs are weird
    val authValue = AuthRepo().authenticate(institutionUUID, userkey.replaceAll(" ", "\\+"), password)
    val authClientType = AuthClientType.valueOf(clientType)
    if (authValue.isDefined) {
      if (authValue.get._2) {
        //forced password reset flag is on
        throw new AuthenticationException("mustUpdatePassword")
      }
      val personUUID = authValue.get._1
      val token = TokenRepo.getToken(personUUID)
      if (token.isDefined) {
        if (token.get.getExpiry == null || token.get.getExpiry.after(new Date)) {
          //token exists and still valid
          token.get
        } else {
          //token expired, we delete old one and create a new one
          TokenRepo().deleteToken(token.get.getToken)
          TokenRepo.createToken(UUID.random(), personUUID, authClientType)
        }
      } else {
        //token did not exist, we create one
        TokenRepo.createToken(UUID.random(), personUUID, authClientType)
      }
    } else {
      //throw login exception
      throw new UnauthorizedAccessException("authenticationFailed")
    }
  }

  @POST
  @Path("logout")
  def logout(@Context req: HttpServletRequest) = {
    val auth = req.getHeader("X-KNL-TOKEN")
    if (auth != null && auth.length() > 0) {
      TokenRepo().deleteToken(auth)
    } else {
      //Gotta be authenticated to logout!
      throw new UnauthorizedAccessException("mustAuthenticate")
    }
    Response.noContent.build
  }

}
