SET @sql = (SELECT IF(
    (SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS WHERE
          table_schema=DATABASE() 
          AND table_name='Institution'
          AND column_name='showPlatformPanel'
    ) > 0,
    "SELECT 0",
    "alter table Institution add showPlatformPanel tinyint unsigned not null default 0;"
));
PREPARE stmt FROM @sql;
EXECUTE stmt;

CREATE TABLE IF NOT EXISTS PlatformConfig (
  id TINYINT AUTO_INCREMENT,
  config TEXT NOT NULL,
  PRIMARY KEY (id)
);