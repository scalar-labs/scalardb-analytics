CREATE TABLE supported_types (
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
  boolean_col BOOLEAN,
  date_col DATE,
  timestamp_col TIMESTAMP,
  timestamp_with_time_zone_col TIMESTAMP WITH TIME ZONE,
  timestamp_with_local_time_zone_col TIMESTAMP WITH LOCAL TIME ZONE,
  raw_col RAW(10),
  not_null_col NUMBER NOT NULL
);

CREATE TABLE unsupported_types (
  float_col FLOAT,
  float_with_precision_54_col FLOAT(54),
  number_with_precision_16_col NUMBER(16, 0),
  interval_year_to_month_col INTERVAL YEAR TO MONTH,
  interval_day_to_second_col INTERVAL DAY TO SECOND,
  row_id_col ROWID,
  urow_id_col UROWID,
  bfile_col BFILE,
  json_col JSON,
  vector_col VECTOR,
  long_raw_col LONG RAW
);

CREATE TABLE long_col_table(
  long_col LONG
);
