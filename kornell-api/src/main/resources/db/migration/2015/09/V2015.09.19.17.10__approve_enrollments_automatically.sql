SET @sql = (SELECT IF(
    (SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS WHERE
          table_schema=DATABASE() 
          AND table_name='CourseClass' 
          AND column_name='approveEnrollmentsAutomatically'
    ) > 0,
    "SELECT 0",
    "alter table CourseClass add column approveEnrollmentsAutomatically tinyint(1) not null default 0;"
));
PREPARE stmt FROM @sql;
EXECUTE stmt;