CREATE CATALOG schema_resolver_test_catalog1;
CREATE CATALOG schema_resolver_test_catalog2;

USE CATALOG schema_resolver_test_catalog1;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE CATALOG　schema_resolver_test_catalog2;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE CATALOG schema_resolver_test_catalog1;

CREATE TABLE schema1.supported_types(
  tinyint_col TINYINT,
  smallint_col SMALLINT,
  int_col INT,
  bigint_col BIGINT,
  float_col FLOAT,
  double_col DOUBLE,
  decimal_00_col DECIMAL,
  decimal_20_col DECIMAL(2,0),
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_21_col DECIMAL(2,1),
  string_col STRING,
  binary_col BINARY,
  boolean_col BOOLEAN,
  date_col DATE,
  timestamp_col TIMESTAMP,
  timestamp_ntz_col TIMESTAMP_NTZ,
  not_null_col INT NOT NULL
);

CREATE TABLE schema1.unsupported_types(
  interval_year_col INTERVAL YEAR,
  interval_month_col INTERVAL MONTH,
  interval_day_col INTERVAL DAY,
  interval_hour_col INTERVAL HOUR,
  interval_minute_col INTERVAL MINUTE,
  interval_second_col INTERVAL SECOND,
  array_col ARRAY<BOOLEAN>,
  map_col MAP<STRING, BOOLEAN>,
  struct_col STRUCT<field1: STRING, field2: INT>,
  variant_col VARIANT
);

USE CATALOG schema_resolver_test_catalog2;

CREATE TABLE schema1.some_table(
  id INT
);

USE CATALOG hive_metastore;

CREATE SCHEMA schema_resolver_test_schema_hive1;
CREATE SCHEMA schema_resolver_test_schema_hive2;

USE SCHEMA schema_resolver_test_schema_hive1;

CREATE TABLE schema_resolver_test_schema_hive1.supported_types_hive(
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
  string_col STRING,
  binary_col BINARY,
  boolean_col BOOLEAN,
  date_col DATE,
  timestamp_col TIMESTAMP,
  timestamp_ntz_col TIMESTAMP_NTZ,
  not_null_col INT NOT NULL
);

CREATE TABLE schema_resolver_test_schema_hive1.unsupported_types_hive(
  decimal_21_col DECIMAL(2,1),
  interval_year_col INTERVAL YEAR,
  interval_month_col INTERVAL MONTH,
  interval_day_col INTERVAL DAY,
  interval_hour_col INTERVAL HOUR,
  interval_minute_col INTERVAL MINUTE,
  interval_second_col INTERVAL SECOND,
  array_col ARRAY<BOOLEAN>,
  map_col MAP<STRING, BOOLEAN>,
  struct_col STRUCT<field1: STRING, field2: INT>,
  variant_col VARIANT
);

