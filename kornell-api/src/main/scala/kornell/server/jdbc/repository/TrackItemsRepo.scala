package kornell.server.jdbc.repository

import java.util.UUID

import kornell.server.jdbc.SQL._
import kornell.core.entity.TrackItem

object TrackItemsRepo {

  def create(trackItem: TrackItem): TrackItem = {
    if (trackItem.getUUID == null) {
      trackItem.setUUID(UUID.randomUUID.toString)
    }
    sql"""
      insert into TrackItem (uuid, courseVersionUUID, trackUUID, parentUUID, `order`, havingPreRequirements, startDate) values
      | (${trackItem.getUUID},
      | ${trackItem.getCourseVersionUUID},
      | ${trackItem.getTrackUUID},
      | ${trackItem.getParentUUID},
      | ${trackItem.getOrder},
      | ${trackItem.isHavingPreRequirements},
      | ${trackItem.getStartDate})
    """.executeUpdate

    trackItem
  }
}
