CREATE SCHEMA IF NOT EXISTS test;
CREATE SCHEMA IF NOT EXISTS test2;

CREATE TABLE IF NOT EXISTS test.test_table (
  smallint_col SMALLINT,
  integer_col INTEGER,
  bigint_col BIGINT,
  real_col REAL,
  double_col DOUBLE PRECISION,
  smallserial_col SMALLSERIAL,
  serial_col SERIAL,
  bigserial_col BIGSERIAL,
  char_col CHAR(10),
  varchar_col VARCHAR(12),
  text_col TEXT,
  bpchar_col BPCHAR,
  boolean_col BOOLEAN,
  bytea_col BYTEA,
  date_col DATE,
  time_col TIME,
  time_with_timezone_col TIME WITH TIME ZONE,
  time_without_timezone_col TIME WITHOUT TIME ZONE,
  timestamp_col TIMESTAMP,
  timestamp_with_timezone_col TIMESTAMP WITH TIME ZONE,
  timestamp_without_timezone_col TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO test.test_table (
  smallint_col,
  integer_col,
  bigint_col,
  real_col,
  double_col,
  char_col,
  varchar_col,
  text_col,
  bpchar_col,
  boolean_col,
  bytea_col,
  date_col,
  time_col,
  time_with_timezone_col,
  time_without_timezone_col,
  timestamp_col,
  timestamp_with_timezone_col,
  timestamp_without_timezone_col
) VALUES (
  32767, -- smallint_col: a small integer
  2147483647, -- integer_col: an integer
  9223372036854775807, -- bigint_col: a big integer
  123.45, -- real_col: a real number
  12345.6789, -- double_col: a double precision number
  'CHAR_TEXT', -- char_col: a CHAR type string
  'VARCHAR_TXT', -- varchar_col: a VARCHAR type string
  'Sample text for TEXT', -- text_col: TEXT type string
  'BPCHAR_TXT', -- bpchar_col: BPCHAR type string
  TRUE, -- boolean_col: a BOOLEAN value
  E'\\x48656c6c6f', -- bytea_col: BYTEA data in hexadecimal (represents 'Hello')
  '2024-10-27', -- date_col: a DATE value
  '15:30:00', -- time_col: a TIME value
  '15:30:00+00', -- time_with_timezone_col: TIME with time zone
  '15:30:00', -- time_without_timezone_col: TIME without time zone
  '2024-10-27 15:30:00', -- timestamp_col: a TIMESTAMP value
  '2024-10-27 15:30:00+00', -- timestamp_with_timezone_col: TIMESTAMP with time zone
  '2024-10-27 15:30:00' -- timestamp_without_timezone_col: TIMESTAMP without time zone
);
