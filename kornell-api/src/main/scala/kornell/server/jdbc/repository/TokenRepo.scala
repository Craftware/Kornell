package kornell.server.jdbc.repository

import kornell.core.entity.AuthClientType
import kornell.server.jdbc.SQL._
import org.joda.time.DateTime
import kornell.server.repository.TOs
import kornell.core.to.TokenTO
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit.MINUTES
import com.google.common.cache.LoadingCache
import com.google.common.cache.CacheLoader


object TokenRepo {
  
  val TOKEN_CACHE_SIZE = 300;

  val cacheBuilder = CacheBuilder
    .newBuilder()
    .expireAfterAccess(15, MINUTES)
    .maximumSize(TOKEN_CACHE_SIZE)
    
  def apply() = new TokenRepo(newTokenCache)
  
  def newTokenCache() = cacheBuilder.build(tokenLoader)
  
  type TokenID = String
  type TokenCache = LoadingCache[TokenID, Option[TokenTO]]
  
  val tokenLoader = new CacheLoader[TokenID, Option[TokenTO]]() {
    override def load(token: TokenID): Option[TokenTO] =
      lookupToken(token)
  }

  def lookupToken(token: TokenID) = {
	sql"""select * from Token where token = ${token}""".first[TokenTO]
  }
  
  def getToken(personUUID: String) = {
    sql"""select * from Token where personUUID = ${personUUID}""".first[TokenTO]
  }
  
  def createToken(token: String, personUUID: String, authClientType: AuthClientType) = {
    val expiry = {
      if (authClientType == AuthClientType.web) {
        new DateTime().plusDays(7).toDate
      } else {
        null
      }
    }
    sql"""insert into Token (token, personUUID, expiry, clientType) values
      	(${token}, ${personUUID}, ${expiry}, ${authClientType.toString})""".executeUpdate
      	
    TOs.newTokenTO(token, expiry, personUUID, authClientType)
  }
}

class TokenRepo(tokenCache: TokenRepo.TokenCache) {
  def checkToken(tokenId: String) = {
    tokenCache.get(tokenId)
  }
  
  def deleteToken(token: String) = {
    sql"""delete from Token where token = ${token}""".executeUpdate
    tokenCache.invalidate(token)
  }
}
