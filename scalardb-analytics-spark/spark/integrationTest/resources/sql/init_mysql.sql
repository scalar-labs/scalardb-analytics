CREATE DATABASE IF NOT EXISTS testdb;
CREATE DATABASE IF NOT EXISTS testdb2;

GRANT ALL PRIVILEGES ON testdb.* TO 'test'@'%';
GRANT ALL PRIVILEGES ON testdb2.* TO 'test'@'%';

CREATE TABLE IF NOT EXISTS testdb.test_table (
  bit_col BIT,
  bit1_col BIT(1),
  bit2_col BIT(2),
  tinyint_col TINYINT,
  tinyint1_col TINYINT(1),
  boolean_col BOOLEAN,
  smallint_col SMALLINT,
  smallint_unsigned_col SMALLINT UNSIGNED,
  mediumint_col MEDIUMINT,
  mediumint_unsigned_col MEDIUMINT UNSIGNED,
  int_col INT,
  int_unsigned_col INT UNSIGNED,
  bigint_col BIGINT,
  float_col FLOAT,
  double_col DOUBLE,
  real_col REAL,
  char_col char(10),
  varchar_col varchar(12),
  tinytext_col TINYTEXT,
  text_col text,
  mediumtext_col MEDIUMTEXT,
  longtext_col LONGTEXT,
  binary_col binary(15),
  varbinary_col varbinary(15),
  tinyblob_col TINYBLOB,
  blob_col blob,
  mediumblob_col MEDIUMBLOB,
  longblob_col LONGBLOB,
  date_col date,
  time_col time,
  datetime_col datetime,
  timestamp_col timestamp
);

INSERT INTO testdb.test_table (
  bit_col,
  bit1_col,
  bit2_col,
  tinyint_col,
  tinyint1_col,
  boolean_col,
  smallint_col,
  smallint_unsigned_col,
  mediumint_col,
  mediumint_unsigned_col,
  int_col,
  int_unsigned_col,
  bigint_col,
  float_col,
  double_col,
  real_col,
  char_col,
  varchar_col,
  tinytext_col,
  text_col,
  mediumtext_col,
  longtext_col,
  binary_col,
  varbinary_col,
  tinyblob_col,
  blob_col,
  mediumblob_col,
  longblob_col,
  date_col,
  time_col,
  datetime_col,
  timestamp_col
) VALUES (
  b'1',                          -- bit_col
  b'1',                          -- bit1_col
  b'10',                         -- bit2_col
  127,                           -- tinyint_col
  1,                             -- tinyint1_col (Tinyint(1) is often treated as a boolean)
  TRUE,                          -- boolean_col
  32767,                         -- smallint_col
  65535,                         -- smallint_unsigned_col
  8388607,                       -- mediumint_col
  16777215,                      -- mediumint_unsigned_col
  2147483647,                    -- int_col
  4294967295,                    -- int_unsigned_col
  9223372036854775807,           -- bigint_col
  4.14,                          -- float_col
  4.14159265358979,              -- double_col
  4.14159,                       -- real_col (equivalent to double in MySQL)
  'char_test',                   -- char_col
  'varchar_test',                -- varchar_col
  'tiny text data',               -- tinytext_col
  'This is a sample text',       -- text_col
  'medium text data',            -- mediumtext_col
  'long text data',              -- longtext_col
  BINARY 'binarydat',            -- binary_col
  BINARY 'varbin_dat',           -- varbinary_col
  'tinyblob_data',               -- tinyblob_col
  'blob_data',                   -- blob_col
  'mediumblob_data',             -- mediumblob_col
  'longblob_data',               -- longblob_col
  '2024-10-27',                  -- date_col
  '12:34:56',                    -- time_col
  '2024-10-27 12:34:56',         -- datetime_col
  '2024-10-27 12:34:56'          -- timestamp_col
);
select * from testdb.test_table;
