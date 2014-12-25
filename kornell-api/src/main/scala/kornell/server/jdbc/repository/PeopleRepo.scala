package kornell.server.jdbc.repository

import kornell.core.entity.Person
import kornell.server.repository.Entities
import kornell.server.jdbc.SQL._
import kornell.server.repository.Entities._
import java.sql.ResultSet
import kornell.core.util.UUID
import kornell.server.repository.TOs
import com.google.common.cache.CacheLoader
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit._
import kornell.core.util.StringUtils._
import kornell.server.jdbc.PreparedStmt
import scala.language.implicitConversions
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Instance
import javax.inject.Inject

//TODO: Urgent: Cache
@ApplicationScoped
class PeopleRepo @Inject() (
  val personRepoBean: Instance[PersonRepo]) {
  def this() = this(null)

  def byUUID(uuid: String) =
    personRepoBean.get.withUUID(uuid)

  implicit def toString(rs: ResultSet): String = rs.getString(1)

  type InstitutionKey = (String, String)

  val usernameLoader = new CacheLoader[InstitutionKey, Option[Person]]() {
    override def load(instKey: InstitutionKey): Option[Person] = lookupByUsername(instKey._1, instKey._2)
  }

  val cpfLoader = new CacheLoader[InstitutionKey, Option[Person]]() {
    override def load(instKey: InstitutionKey): Option[Person] = lookupByCPF(instKey._1, instKey._2)
  }

  val emailLoader = new CacheLoader[InstitutionKey, Option[Person]]() {
    override def load(instKey: InstitutionKey): Option[Person] = lookupByEmail(instKey._1, instKey._2)
  }

  val uuidLoader = new CacheLoader[String, Option[Person]]() {
    override def load(uuid: String): Option[Person] = byUUID(uuid).first
  }

  val DEFAULT_CACHE_SIZE = 1000

  val cacheBuilder = CacheBuilder
    .newBuilder()
    .expireAfterAccess(5, MINUTES)
    .maximumSize(1000)

  val usernameCache = cacheBuilder.build(usernameLoader)

  val cpfCache = cacheBuilder.build(cpfLoader)

  val emailCache = cacheBuilder.build(emailLoader)

  val uuidCache = cacheBuilder.build(uuidLoader)

  def getByUsername(institutionUUID: String, username: String) = Option(institutionUUID, username) flatMap usernameCache.get
  def getByEmail(institutionUUID: String, email: String) = Option(institutionUUID, email) flatMap emailCache.get
  def getByCPF(institutionUUID: String, cpf: String) = Option(institutionUUID, cpf) flatMap cpfCache.get
  def getByUUID(uuid: String) = uuidCache.get(uuid)

  def lookupByUsername(institutionUUID: String, username: String) = sql"""
		select p.* from Person p
		join Password pwd
		on p.uuid = pwd.person_uuid
		where pwd.username = $username
		and p.institutionUUID = $institutionUUID
	""".first[Person]

  def lookupByCPF(institutionUUID: String, cpf: String) = sql"""
		select p.* from Person p	
		where p.cpf = $cpf
		and p.institutionUUID = $institutionUUID
	""".first[Person]

  def lookupByEmail(institutionUUID: String, email: String) = sql"""
		select p.* from Person p	
		where p.email = $email
		and p.institutionUUID = $institutionUUID
	""".first[Person]

  def get(institutionUUID: String, any: String): Option[Person] = get(institutionUUID, any, any, any)

  def get(institutionUUID: String, cpf: String, email: String): Option[Person] =
    getByUsername(institutionUUID, {
      if (cpf == null)
        cpf
      else
        email
    })
      .orElse(getByCPF(institutionUUID, cpf))
      .orElse(getByEmail(institutionUUID, email))

  def get(institutionUUID: String, username: String, cpf: String, email: String): Option[Person] =
    getByUsername(institutionUUID, username)
      .orElse(getByCPF(institutionUUID, cpf))
      .orElse(getByEmail(institutionUUID, email))

  def findBySearchTerm(institutionUUID: String, search: String) = {
    newPeople(
      sql"""
      	| select p.* from Person p 
      	| join Password pw on p.uuid = pw.person_uuid
      	| where (pw.username like ${"%" + search + "%"}
      	| or p.fullName like ${"%" + search + "%"}
      	| or p.email like ${"%" + search + "%"}
      	| or p.cpf like ${"%" + search + "%"})
      	| and p.institutionUUID = ${institutionUUID}
      	| order by p.email, p.cpf
      	| limit 8
	    """.map[Person](toPerson))
  }

  def createPerson(institutionUUID: String = null, email: String = null, fullName: String = null, cpf: String = null): Person =
    create(Entities.newPerson(institutionUUID = institutionUUID,
      fullName = fullName,
      email = email,
      cpf = cpf))

  def createPersonCPF(institutionUUID: String, cpf: String, fullName: String): Person =
    create(Entities.newPerson(institutionUUID = institutionUUID, fullName = fullName, cpf = cpf))

  def createPersonUsername(institutionUUID: String, username: String, fullName: String): Person = {
    val p = create(Entities.newPerson(institutionUUID = institutionUUID, fullName = fullName))
    if (isSome(username)) usernameCache.put((p.getInstitutionUUID, username), Some(p))
    p
  }
  

  def create(person: Person): Person = {
    if (person.getUUID == null)
      person.setUUID(randUUID)
    sql""" 
    	insert into Person(uuid, fullName, email, cpf, institutionUUID) 
    		values (${person.getUUID},
	             ${person.getFullName},
	             ${person.getEmail},
	             ${person.getCPF},
	             ${person.getInstitutionUUID})
    """.executeUpdate
    updateCaches(person)
    logger.fine(s"Created Person[${person.getUUID()}]")
    person
  }

  def updateCaches(p: Person) = {
    val op = Some(p)
    uuidCache.put(p.getUUID, op)
    if (isSome(p.getCPF)) cpfCache.put((p.getInstitutionUUID, p.getCPF), op)
    if (isSome(p.getEmail)) emailCache.put((p.getInstitutionUUID, p.getEmail), op)
  }

}
