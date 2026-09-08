CREATE CATALOG spark_embedded_test_catalog1;
CREATE CATALOG spark_embedded_test_catalog2;

USE CATALOG spark_embedded_test_catalog1;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE CATALOG spark_embedded_test_catalog2;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE CATALOG spark_embedded_test_catalog1;

CREATE TABLE schema1.test_table(
  tinyint_col TINYINT,
  smallint_col SMALLINT,
  int_col INT,
  bigint_col BIGINT,
  float_col FLOAT,
  double_col DOUBLE,
  decimal_20_col DECIMAL(2,0),
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_380_col DECIMAL(38,0),
  string_col STRING,
  binary_col BINARY,
  boolean_col BOOLEAN,
  date_col DATE,
  timestamp_col TIMESTAMP,
  timestamp_ntz_col TIMESTAMP_NTZ
);

INSERT INTO schema1.test_table (
  tinyint_col,
  smallint_col,
  int_col,
  bigint_col,
  float_col,
  double_col,
  decimal_20_col,
  decimal_30_col,
  decimal_40_col,
  decimal_50_col,
  decimal_90_col,
  decimal_100_col,
  decimal_180_col,
  decimal_190_col,
  decimal_380_col,
  string_col,
  binary_col,
  boolean_col,
  date_col,
  timestamp_col,
  timestamp_ntz_col
) VALUES (
  127Y, -- tinyint_col: a TINYINT value
  32767, -- smallint_col: a SMALLINT value
  2147483647, -- int_col: a INT value
  9223372036854775807, -- bigint_col: a BIGINT value
  3.402E38, -- float_col: a FLOAT value
  1.79769E308, -- double_col: a DOUBLE value
  99, -- decimal_20_col: a DECIMAL(2,0) value
  100, -- decimal_30_col: a DECIMAL(3,0) value
  9999, -- decimal_40_col: an DECIMAL(4,0) value
  10000, -- decimal_50_col: a DECIMAL(5,0) value
  999999999, -- decimal_90_col: a DECIMAL(9,0) value
  1E9BD, -- decimal_100_col: a DECIMAL(10,0) value
  999999999999999999, -- decimal_180_col: a DECIMAL(18,0) value
  1E18BD, -- decimal_190_col: a DECIMAL(19,0) value
  1E37BD, -- decimal_380_col: a DECIMAL(38,0) value
  'foo bar baz', -- string_col: a STRING value
  X'1ABF', -- binary_col: a BINARY value
  true, -- boolean_col: a BOOLEAN value
  '2000-7-14', -- date_col: a DATE value
  '2000-7-14T12:34:56.123', -- timestamp_col: a TIMESTAMPZ value
  '2000-7-14T12:34:56.123456' -- timestamp_ntz_col: a TIMESTAMP_NTZ value
);

USE CATALOG hive_metastore;

CREATE SCHEMA spark_embedded_test_schema_hive1;
CREATE SCHEMA spark_embedded_test_schema_hive2;

CREATE TABLE spark_embedded_test_schema_hive1.test_table_hive(
  tinyint_col TINYINT,
  smallint_col SMALLINT,
  int_col INT,
  bigint_col BIGINT,
  float_col FLOAT,
  double_col DOUBLE,
  decimal_20_col DECIMAL(2,0),
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_380_col DECIMAL(38,0),
  string_col STRING,
  binary_col BINARY,
  boolean_col BOOLEAN,
  date_col DATE,
  timestamp_col TIMESTAMP,
  timestamp_ntz_col TIMESTAMP_NTZ
);

INSERT INTO spark_embedded_test_schema_hive1.test_table_hive (
  tinyint_col,
  smallint_col,
  int_col,
  bigint_col,
  float_col,
  double_col,
  decimal_20_col,
  decimal_30_col,
  decimal_40_col,
  decimal_50_col,
  decimal_90_col,
  decimal_100_col,
  decimal_180_col,
  decimal_190_col,
  decimal_380_col,
  string_col,
  binary_col,
  boolean_col,
  date_col,
  timestamp_col,
  timestamp_ntz_col
) VALUES (
  127Y, -- tinyint_col: a TINYINT value
  32767, -- smallint_col: a SMALLINT value
  2147483647, -- int_col: a INT value
  9223372036854775807, -- bigint_col: a BIGINT value
  3.402E38, -- float_col: a FLOAT value
  1.79769E308, -- double_col: a DOUBLE value
  99, -- decimal_20_col: a DECIMAL(2,0) value
  100, -- decimal_30_col: a DECIMAL(3,0) value
  9999, -- decimal_40_col: an DECIMAL(4,0) value
  10000, -- decimal_50_col: a DECIMAL(5,0) value
  999999999, -- decimal_90_col: a DECIMAL(9,0) value
  1E9BD, -- decimal_100_col: a DECIMAL(10,0) value
  999999999999999999, -- decimal_180_col: a DECIMAL(18,0) value
  1E18BD, -- decimal_190_col: a DECIMAL(19,0) value
  1E37BD, -- decimal_380_col: a DECIMAL(38,0) value
  'foo bar baz', -- string_col: a STRING value
  X'1ABF', -- binary_col: a BINARY value
  true, -- boolean_col: a BOOLEAN value
  '2000-7-14', -- date_col: a DATE value
  '2000-7-14T12:34:56.123', -- timestamp_col: a TIMESTAMPZ value
  '2000-7-14T12:34:56.123456' -- timestamp_ntz_col: a TIMESTAMP_NTZ value
);
