CREATE DATABASE schema_resolver_test_database1;
CREATE DATABASE schema_resolver_test_database2;

USE DATABASE schema_resolver_test_database1;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE DATABASE schema_resolver_test_database2;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE DATABASE schema_resolver_test_database1;

CREATE TABLE schema1.supported_types(
  decimal_20_col DECIMAL(2,0),
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_380_col DECIMAL(38,0),
  decimal_21_col DECIMAL(2,1),
  number_col NUMBER(36,0), --alias of DECIMAL(p,s)
  numeric_col NUMERIC(33,0), --alias of DECIMAL(p,s)
  int_col INT, --alias of DECIMAL(38,0)
  integer_col INTEGER, --alias of DECIMAL(38,0)
  bigint_col BIGINT, --alias of DECIMAL(38,0)
  smallint_col SMALLINT, --alias of DECIMAL(38,0)
  tinyint_col TINYINT, --alias of DECIMAL(38,0)
  byteint_col BYTEINT, --alias of DECIMAL(38,0)
  float_col FLOAT,
  float4_col FLOAT4, --alias of FLOAT
  float8_col FLOAT8, --alias of FLOAT
  double_col DOUBLE, --alias of FLOAT
  double_precision_col DOUBLE PRECISION, --alias of FLOAT
  real_col REAL, --alias of FLOAT
  varchar_col VARCHAR(100),
  string_col STRING(100), --alias of VARCHAR(l)
  text_col TEXT(100), --alias of VARCHAR(l)
  nvarchar_col NVARCHAR(100), --alias of VARCHAR(l)
  nvarchar2_col NVARCHAR2(100), --alias of VARCHAR(l)
  char_varying_col CHAR VARYING(100), --alias of VARCHAR(l)
  nchar_varying_col NCHAR VARYING(100), --alias of VARCHAR(l)
  char_col CHAR, --alias of VARCHAR(1)
  character_col CHARACTER, --alias of VARCHAR(1)
  nchar_col NCHAR, --alias of VARCHAR(1)
  binary_col BINARY(100),
  varbinary_col VARBINARY(100), --alias of BINARY(l)
  boolean_col BOOLEAN,
  date_col DATE,
  time_col TIME,
  timestamp_ntz_col TIMESTAMP_NTZ,
  datetime_col DATETIME, -- alias of TIMESTAMP_NTZ
  timestamp_ltz_col TIMESTAMP_LTZ,
  timestamp_tz_col TIMESTAMP_TZ,
  not_null_col INT NOT NULL
);

CREATE TABLE schema1.unsupported_types(
  array_col ARRAY,
  object_col OBJECT,
  variant_col VARIANT,
  geography_col GEOGRAPHY,
  geometry_col GEOMETRY,
  vector_col VECTOR(INT,3)
);

CREATE HYBRID TABLE schema1.supported_types_hybrid(
  decimal_20_col DECIMAL(2,0) PRIMARY KEY,
  decimal_30_col DECIMAL(3,0),
  decimal_40_col DECIMAL(4,0),
  decimal_50_col DECIMAL(5,0),
  decimal_90_col DECIMAL(9,0),
  decimal_100_col DECIMAL(10,0),
  decimal_180_col DECIMAL(18,0),
  decimal_190_col DECIMAL(19,0),
  decimal_380_col DECIMAL(38,0),
  number_col NUMBER(36,0), --alias of DECIMAL(p,s)
  numeric_col NUMERIC(33,0), --alias of DECIMAL(p,s)
  int_col INT, --alias of DECIMAL(38,0)
  integer_col INTEGER, --alias of DECIMAL(38,0)
  bigint_col BIGINT, --alias of DECIMAL(38,0)
  smallint_col SMALLINT, --alias of DECIMAL(38,0)
  tinyint_col TINYINT, --alias of DECIMAL(38,0)
  byteint_col BYTEINT, --alias of DECIMAL(38,0)
  float_col FLOAT,
  float4_col FLOAT4, --alias of FLOAT
  float8_col FLOAT8, --alias of FLOAT
  double_col DOUBLE, --alias of FLOAT
  double_precision_col DOUBLE PRECISION, --alias of FLOAT
  real_col REAL, --alias of FLOAT
  varchar_col VARCHAR(100),
  string_col STRING(100), --alias of VARCHAR(l)
  text_col TEXT(100), --alias of VARCHAR(l)
  nvarchar_col NVARCHAR(100), --alias of VARCHAR(l)
  nvarchar2_col NVARCHAR2(100), --alias of VARCHAR(l)
  char_varying_col CHAR VARYING(100), --alias of VARCHAR(l)
  nchar_varying_col NCHAR VARYING(100), --alias of VARCHAR(l)
  char_col CHAR, --alias of VARCHAR(1)
  character_col CHARACTER, --alias of VARCHAR(1)
  nchar_col NCHAR, --alias of VARCHAR(1)
  binary_col BINARY(100),
  varbinary_col VARBINARY(100), --alias of BINARY(l)
  boolean_col BOOLEAN,
  date_col DATE,
  time_col TIME,
  timestamp_ntz_col TIMESTAMP_NTZ,
  datetime_col DATETIME, -- alias of TIMESTAMP_NTZ
  timestamp_ltz_col TIMESTAMP_LTZ,
  timestamp_tz_col TIMESTAMP_TZ,
  not_null_col INT NOT NULL
);

CREATE HYBRID TABLE schema1.unsupported_types_hybrid(
  ignored_col INT PRIMARY KEY, -- setting a primary key is required but other unsupported types cannot be used as primary key
  array_col ARRAY,
  object_col OBJECT,
  variant_col VARIANT,
  geography_col GEOGRAPHY,
  geometry_col GEOMETRY,
  vector_col VECTOR(INT,3)
);

USE DATABASE schema_resolver_test_database2;

CREATE TABLE schema1.some_table(
  id INT
);
