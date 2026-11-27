CREATE SCHEMA IF NOT EXISTS test;
CREATE SCHEMA IF NOT EXISTS test2;

CREATE TABLE IF NOT EXISTS test.supported_types (
  smallint_col SMALLINT,
  integer_col INTEGER,
  bigint_col BIGINT,
  real_col REAL,
  double_col DOUBLE PRECISION,
  smallserial_col SMALLSERIAL,
  serial_col SERIAL,
  bigserial_col BIGSERIAL,
  char_col CHAR(10),
  varchar_col VARCHAR(10),
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
  timestamp_without_timezone_col TIMESTAMP WITHOUT TIME ZONE,
  not_null_col INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS test.unsupported_types (
  json_col JSON,
  decimal_col DECIMAL,
  numeric_col NUMERIC,
  money_col MONEY,
  interval_col INTERVAL,
  uuid_col UUID,
  cidr_col CIDR,
  inet_col INET,
  macaddr_col MACADDR,
  macaddr8_col MACADDR8,
  xml_col XML
);
