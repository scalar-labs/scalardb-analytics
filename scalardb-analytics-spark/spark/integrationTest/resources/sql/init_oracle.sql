-- Set session timezone to UTC for consistent TIMESTAMP WITH LOCAL TIME ZONE behavior
ALTER SESSION SET TIME_ZONE = 'UTC';

-- Create TESTUSER schema and grant necessary privileges
CREATE USER TESTUSER IDENTIFIED BY test;
GRANT CONNECT, RESOURCE TO TESTUSER;
GRANT CREATE SESSION TO TESTUSER;
GRANT CREATE TABLE TO TESTUSER;
GRANT UNLIMITED TABLESPACE TO TESTUSER;

-- Create table in TESTUSER schema
CREATE TABLE TESTUSER.test_table (
  number_col NUMBER,
  number_without_scale_col NUMBER(15,0),
  number_with_scale_col NUMBER(15,10),
  float_with_precision_53_col FLOAT(53),
  binary_float_col BINARY_FLOAT,
  binary_double_col BINARY_DOUBLE,
  char_col CHAR(10),
  nchar_col NCHAR(10),
  varchar2_col VARCHAR2(10),
  nvarchar2_col NVARCHAR2(10),
  clob_col CLOB,
  nclob_col NCLOB,
  blob_col BLOB,
  date_col DATE,
  timestamp_col TIMESTAMP,
  timestamp_with_time_zone_col TIMESTAMP WITH TIME ZONE,
  timestamp_with_local_time_zone_col TIMESTAMP WITH LOCAL TIME ZONE,
  raw_col RAW(12),
  boolean_col BOOLEAN
);

INSERT INTO TESTUSER.test_table (
  number_col,
  number_without_scale_col,
  number_with_scale_col,
  float_with_precision_53_col,
  binary_float_col,
  binary_double_col,
  char_col,
  nchar_col,
  varchar2_col,
  nvarchar2_col,
  clob_col,
  nclob_col,
  blob_col,
  date_col,
  timestamp_col,
  timestamp_with_time_zone_col,
  timestamp_with_local_time_zone_col,
  raw_col,
  boolean_col
) VALUES (
  12345, -- number_col: an integer
  123456789012345, -- number_without_scale_col: a large integer with no scale
  123.4567890123, -- number_with_scale_col: a number with scale up to 10 decimal places
  12345.6789, -- float_with_precision_53_col: a FLOAT value with precision
  1.23f, -- binary_float_col: a BINARY_FLOAT value
  123.45678, -- binary_double_col: a BINARY_DOUBLE value
  'CHAR_TEXT', -- char_col: a CHAR type string
  N'NCHARテキスト', -- nchar_col: an NCHAR type string with Japanese characters
  'VCHAR_TEXT', -- varchar2_col: a VARCHAR2 type string
  N'NVCHARテキスト', -- nvarchar2_col: an NVARCHAR2 type string with Japanese characters
  'CLOBサンプルテキスト', -- clob_col: CLOB type text with Japanese characters
  N'NCLOBサンプルテキスト', -- nclob_col: NCLOB type text with Japanese characters
  UTL_RAW.CAST_TO_RAW('BLOBデータ'), -- blob_col: BLOB data converted from text with Japanese characters
  DATE '2024-10-27', -- date_col: a DATE value
  TIMESTAMP '2024-10-27 15:30:00', -- timestamp_col: a TIMESTAMP value
  TIMESTAMP '2024-10-27 15:30:00 +00:00', -- timestamp_with_time_zone_col: TIMESTAMP with time zone
  TIMESTAMP '2024-10-27 15:30:00', -- timestamp_with_local_time_zone_col: TIMESTAMP with local time zone
  UTL_RAW.CAST_TO_RAW('RAWデータ'), -- raw_col: RAW data with Japanese characters
  TRUE -- boolean_col: a BOOLEAN value
);

COMMIT;
