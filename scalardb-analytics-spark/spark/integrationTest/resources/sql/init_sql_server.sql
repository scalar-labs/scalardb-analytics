CREATE DATABASE testdb;
CREATE DATABASE testdb2;

USE testdb;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE testdb2;

CREATE SCHEMA schema1;
CREATE SCHEMA schema2;

USE testdb;

CREATE TABLE schema1.test_table(
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
  varchar_col VARCHAR(15),
  nchar_col NCHAR(10),
  nvarchar_col NVARCHAR(15),
  text_col TEXT,
  ntext_col NTEXT,
  date_col DATE,
  time_col TIME,
  datetime_col DATETIME,
  datetime2_col DATETIME2,
  smalldatetime_col SMALLDATETIME,
  datetimeoffset_col DATETIMEOFFSET,
);

INSERT INTO schema1.test_table (
  bit_col,
  tinyint_col,
  smallint_col,
  int_col,
  bigint_col,
  real_col,
  float_col,
  float24_col,
  float25_col,
  float53_col,
  binary_col,
  varbinary_col,
  char_col,
  varchar_col,
  nchar_col,
  nvarchar_col,
  text_col,
  ntext_col,
  date_col,
  time_col,
  datetime_col,
  datetime2_col,
  smalldatetime_col,
  datetimeoffset_col
) VALUES (
  1, -- bit_col: a BIT value
  255, -- tinyint_col: maximum TINYINT value
  32767, -- smallint_col: maximum SMALLINT value
  2147483647, -- int_col: maximum INT value
  9223372036854775807, -- bigint_col: maximum BIGINT value
  123.45, -- real_col: a REAL number
  12345.6789, -- float_col: a FLOAT number
  123.45, -- float24_col: a FLOAT(24) number
  12345.6789, -- float25_col: a FLOAT(25) number
  123456.789, -- float53_col: a FLOAT(53) number
  0x1234567890, -- binary_col: BINARY data in hexadecimal
  0xABCDEF, -- varbinary_col: VARBINARY data in hexadecimal
  'CHAR_TEXT', -- char_col: a CHAR type string
  'VARCHAR_TXT', -- varchar_col: a VARCHAR type string
  N'NCHAR_TXT', -- nchar_col: an NCHAR type string
  N'NVARCHAR_TXT', -- nvarchar_col: an NVARCHAR type string
  'Sample text for TEXT', -- text_col: TEXT type text
  N'Sample text for NTEXT', -- ntext_col: NTEXT type text
  '2024-10-27', -- date_col: a DATE value
  '15:30:00', -- time_col: a TIME value
  '2024-10-27 15:30:00', -- datetime_col: a DATETIME value
  '2024-10-27 15:30:00.1234567', -- datetime2_col: a DATETIME2 value with fractional seconds
  '2024-10-27 15:30:00', -- smalldatetime_col: a SMALLDATETIME value
  '2024-10-27 15:30:00 +00:00' -- datetimeoffset_col: a DATETIMEOFFSET value
);
