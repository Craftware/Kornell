package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.Track
import kornell.server.jdbc.SQL._

object TracksRepo {

  def create(track: Track): Track = {
    if (track.getUUID == null) {
      track.setUUID(UUID.randomUUID.toString)
    }
    sql"""
      insert into Track (uuid, institutionUUID, name) values (${track.getUUID}, ${track.getInstitutionUUID}, ${track.getName})
    """.executeUpdate

    track
  }

  def getByInstitution(institutionUUID: String): List[Track] = {
    sql"""select * from Track where institutionUUID = ${institutionUUID}""".map[Track](toTrack)
  }
}
