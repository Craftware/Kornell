SET @sql = (SELECT IF(
    (SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS WHERE
          table_schema=DATABASE() 
          AND table_name='Institution'
          AND column_name='enforceSequentialProgress'
    ) > 0,
    "SELECT 0",
    "alter table Institution add enforceSequentialProgress tinyint unsigned not null default 1;"
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
