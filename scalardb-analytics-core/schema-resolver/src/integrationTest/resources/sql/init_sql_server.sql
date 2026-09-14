CREATE DATABASE testdb;
CREATE DATABASE testdb2;

USE testdb;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE testdb2;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE testdb;

CREATE TABLE schema1.supported_types(
  bit_col BIT,
  tinyint_col TINYINT,
  smallint_col SMALLINT,
  int_col INT,
  bigint_col BIGINT,
  real_col REAL,
  float_col FLOAT,
  float24_col FLOAT(24),
  float25_col FLOAT(25),
  float53_col FLOAT(53),
  binary_col BINARY(10),
  varbinary_col VARBINARY(10),
  char_col CHAR(10),
  varchar_col VARCHAR(10),
  nchar_col NCHAR(10),
  nvarchar_col NVARCHAR(10),
  ntext_col NTEXT,
  text_col TEXT,
  date_col DATE,
  time_col TIME,
  datetime_col DATETIME,
  datetime2_col DATETIME2,
  smalldatetime_col SMALLDATETIME,
  datetimeoffset_col DATETIMEOFFSET,
  not_null_col INT NOT NULL
);

CREATE TABLE schema1.unsupported_types(
  decimal_col DECIMAL(10, 2),
  numeric_col NUMERIC(10, 2),
  money_col MONEY,
  smallmoney_col SMALLMONEY,
  image_col IMAGE,
  uniqueidentifier_col UNIQUEIDENTIFIER,
  xml_col XML,
  geography_col GEOGRAPHY,
  geometry_col GEOMETRY,
  hierarchyid_col HIERARCHYID,
  rowversion_col ROWVERSION,
  sql_variant_col SQL_VARIANT
);

USE testdb2;

CREATE TABLE schema1.some_table(
  id INT
);
