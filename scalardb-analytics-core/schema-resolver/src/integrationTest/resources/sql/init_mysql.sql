CREATE DATABASE IF NOT EXISTS testdb;
CREATE DATABASE IF NOT EXISTS testdb2;

GRANT ALL PRIVILEGES ON testdb.* TO 'test'@'%';
GRANT ALL PRIVILEGES ON testdb2.* TO 'test'@'%';

CREATE TABLE IF NOT EXISTS testdb.supported_types (
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
  varchar_col varchar(10),
  tinytext_col tinytext,
  text_col text,
  mediumtext_col mediumtext,
  longtext_col longtext,
  binary_col binary(10),
  varbinary_col varbinary(10),
  tinyblob_col tinyblob,
  blob_col blob,
  mediumblob_col mediumblob,
  longblob_col longblob,
  date_col date,
  time_col time,
  datetime_col datetime,
  timestamp_col timestamp,
  not_null_col INT NOT NULL
);

CREATE TABLE IF NOT EXISTS testdb.unsupported_types (
  bigint_unsigned_col BIGINT UNSIGNED,
  year_col YEAR,
  decimal_col DECIMAL,
  numeric_col NUMERIC,
  enum_col ENUM('a', 'b', 'c'),
  set_col SET('a', 'b', 'c'),
  json_col JSON,
  geometry_col GEOMETRY
);

CREATE TABLE testdb2.some_table(
  id INT
);
