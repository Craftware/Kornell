package kornell.server.jdbc.repository

import java.util.UUID

import kornell.server.jdbc.SQL._
import kornell.core.entity.TrackEnrollment

object TrackEnrollmentsRepo {

  def create(trackEnrollment: TrackEnrollment): TrackEnrollment = {
    if (trackEnrollment.getUUID == null) {
      trackEnrollment.setUUID(UUID.randomUUID.toString)
    }
    sql"""
      insert into TrackEnrollment (uuid, personUUID, trackUUID) values
      | (${trackEnrollment.getUUID},
      | ${trackEnrollment.getPersonUUID},
      | ${trackEnrollment.getTrackUUID})
    """.executeUpdate

    trackEnrollment
  }

}
