CREATE TABLE IF NOT EXISTS Curriculum (
  institutionUUID char(36) DEFAULT NULL,
  courseUUID char(36) DEFAULT NULL,
  addedOn datetime DEFAULT NULL,
  PRIMARY KEY (institutionUUID, courseUUID),
  KEY institutionUUID (institutionUUID),
  KEY courseUUID (courseUUID),
  CONSTRAINT Curriculum_Institution FOREIGN KEY (institutionUUID) REFERENCES Institution (uuid),
  CONSTRAINT Curriculum_Course FOREIGN KEY (courseUUID) REFERENCES Course (uuid)
);