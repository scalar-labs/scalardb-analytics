CREATE DATABASE spark_embedded_test_catalog1;
CREATE DATABASE spark_embedded_test_catalog2;

USE DATABASE spark_embedded_test_catalog1;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE DATABASE spark_embedded_test_catalog2;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE DATABASE spark_embedded_test_catalog1;

CREATE TABLE schema1.test_table(
  decimal_20_col DECIMAL(2,0),
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_380_col DECIMAL(38,0),
  int_col INT,
  float_col FLOAT,
  varchar_col VARCHAR(100),
  char_col CHAR,
  binary_col BINARY(100),
  boolean_col BOOLEAN,
  date_col DATE,
  time_col TIME,
  timestamp_ntz_col TIMESTAMP_NTZ,
  timestamp_ltz_col TIMESTAMP_LTZ,
  timestamp_tz_col TIMESTAMP_TZ
);

CREATE HYBRID TABLE schema1.test_table_hybrid(
  decimal_20_col DECIMAL(2,0) PRIMARY KEY,
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_380_col DECIMAL(38,0),
  int_col INT,
  float_col FLOAT,
  varchar_col VARCHAR(100),
  char_col CHAR,
  binary_col BINARY(100),
  boolean_col BOOLEAN,
  date_col DATE,
  time_col TIME,
  timestamp_ntz_col TIMESTAMP_NTZ,
  timestamp_ltz_col TIMESTAMP_LTZ,
  timestamp_tz_col TIMESTAMP_TZ
);

ALTER SESSION SET TIMEZONE = "Asia/Hong_Kong";

INSERT INTO schema1.test_table (
  decimal_20_col,
  decimal_30_col,
  decimal_40_col,
  decimal_50_col,
  decimal_90_col,
  decimal_100_col,
  decimal_180_col,
  decimal_190_col,
  decimal_380_col,
  int_col,
  float_col,
  varchar_col,
  char_col,
  binary_col,
  boolean_col,
  date_col,
  time_col,
  timestamp_ntz_col,
  timestamp_ltz_col,
  timestamp_tz_col
) VALUES (
  99, -- decimal_20_col: a DECIMAL(2,0) value
  100, -- decimal_30_col: a DECIMAL(3,0) value
  9999, -- decimal_40_col: an DECIMAL(4,0) value
  10000, -- decimal_50_col: a DECIMAL(5,0) value
  999999999, -- decimal_90_col: a DECIMAL(9,0) value
  1E9, -- decimal_100_col: a DECIMAL(10,0) value
  999999999999999999, -- decimal_180_col: a DECIMAL(18,0) value
  1E18, -- decimal_190_col: a DECIMAL(19,0) value
  1E37, -- decimal_380_col: a DECIMAL(38,0) value
  1E36, -- int_col: an INT value
  3.13, -- float_col: a DOUBLE value
  'foo bar baz', -- varchar_col: a TEXT value
  'c', -- char_col: a TEXT value
  X'1ABF', -- binary_col: a BINARY value
  true, -- boolean_col: a BOOLEAN value
  '2000-7-14', -- date_col: a DATE value
  '12:34:56.123456789', -- time_col: a TIME value fail on 3.14
  '2000-7-14T12:34:56.123456789', -- timestamp_ntz_col: a TIMESTAMP value fail on 3.14
  '2000-7-14T12:34:56.123456789', -- timestamp_ltz_col: a TIMESTAMPTZ value
  '2000-7-14T12:34:56.123456789 +07:00' -- timestamp_tz_col: a TIMESTAMPTZ value
);

INSERT INTO schema1.test_table_hybrid (
  decimal_20_col,
  decimal_30_col,
  decimal_40_col,
  decimal_50_col,
  decimal_90_col,
  decimal_100_col,
  decimal_180_col,
  decimal_190_col,
  decimal_380_col,
  int_col,
  float_col,
  varchar_col,
  char_col,
  binary_col,
  boolean_col,
  date_col,
  time_col,
  timestamp_ntz_col,
  timestamp_ltz_col,
  timestamp_tz_col
) VALUES (
  99, -- decimal_20_col: a DECIMAL(2,0) value
  100, -- decimal_30_col: a DECIMAL(3,0) value
  9999, -- decimal_40_col: an DECIMAL(4,0) value
  10000, -- decimal_50_col: a DECIMAL(5,0) value
  999999999, -- decimal_90_col: a DECIMAL(9,0) value
  1E9, -- decimal_100_col: a DECIMAL(10,0) value
  999999999999999999, -- decimal_180_col: a DECIMAL(18,0) value
  1E18, -- decimal_190_col: a DECIMAL(19,0) value
  1E37, -- decimal_380_col: a DECIMAL(38,0) value
  1E36, -- int_col: an INT value
  3.13, -- float_col: a DOUBLE value
  'foo bar baz', -- varchar_col: a TEXT value
  'c', -- char_col: a TEXT value
  X'1ABF', -- binary_col: a BINARY value
  true, -- boolean_col: a BOOLEAN value
  '2000-7-14', -- date_col: a DATE value
  '12:34:56.123456789', -- time_col: a TIME value fail on 3.14
  '2000-7-14T12:34:56.123456789', -- timestamp_ntz_col: a TIMESTAMP value fail on 3.14
  '2000-7-14T12:34:56.123456789', -- timestamp_ltz_col: a TIMESTAMPTZ value
  '2000-7-14T12:34:56.123456789 +07:00' -- timestamp_tz_col: a TIMESTAMPTZ value
);

