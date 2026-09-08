/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.scalar.db.analytics.api.model.DataType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** This class provides the schema resolution logic for an Oracle data source. */
class SchemaResolverStrategyOracle implements RdbmsSchemaResolverStrategy {
  /**
   * Oracle predefined administrative and non-administrative users. These users are not considered
   * as user schemas and are ignored.
   *
   * <p>Reference: <a
   * href="https://docs.oracle.com/cd/G11854_01/dbseg/managing-security-for-oracle-database-users.html#GUID-B8EF08C7-28E5-492D-A300-C5B1866FDCC8">Oracle
   * Database Document</a>
   */
  private static final ImmutableSet<String> PREDEFINED_ADMIN_USERS =
      ImmutableSet.of(
          "ANONYMOUS",
          "APPQOSSYS",
          "AUDSYS",
          "CTXSYS",
          "DBSNMP",
          "DGPDB_INT",
          "DBSFWUSER",
          "DVF",
          "DVSYS",
          "GGSYS",
          "GSMADMIN_INTERNAL",
          "GSMCATUSER",
          "GSMROOTUSER",
          "GSMUSER",
          "LBACSYS",
          "MDSYS",
          "OJVMSYS",
          "ORDDATA",
          "ORDPLUGINS",
          "ORDSYS",
          "OUTLN",
          "REMOTE_SCHEDULER_AGENT",
          "SI_INFORMTN_SCHEMA",
          "SYS",
          "SYS$UMF",
          "SYSBACKUP",
          "SYSDG",
          "SYSKM",
          "SYSRAC",
          "SYSTEM",
          "WMSYS",
          "XDB");

  private static final ImmutableSet<String> PREDEFINED_NON_ADMIN_USERS =
      ImmutableSet.of("DIP", "MDDATA", "ORACLE_OCM", "XS$NULL");

  /**
   * The predefined users that are not described in the predefined users sections of the Oracle
   * Database documents.
   */
  private static final ImmutableSet<String> PREDEFINED_UNKNOWN_USERS =
      ImmutableSet.of("GGSHAREDCAP", "PDBADMIN", "VECSYS", "BAASSYS");

  private static final ImmutableSet<String> SCHEMAS_TO_IGNORE =
      ImmutableSet.<String>builder()
          .addAll(PREDEFINED_ADMIN_USERS)
          .addAll(PREDEFINED_NON_ADMIN_USERS)
          .addAll(PREDEFINED_UNKNOWN_USERS)
          .build();

  private static final ImmutableSet<String> SCHEMA_PREFIXES_TO_IGNORE = ImmutableSet.of("OPS$");

  /**
   * Resolves all the schemas in the specified Oracle database. The schemas of the predefined users
   * are ignored.
   */
  @Override
  public List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData) throws JdbcException {
    try (ResultSet schemas = metaData.getSchemas()) {
      return JdbcUtil.iterateOverResultSet(
          schemas,
          rs -> {
            String schema = metaData.getSchemasTableSchem(rs);
            if (SCHEMAS_TO_IGNORE.contains(schema)) {
              return null;
            }
            if (SCHEMA_PREFIXES_TO_IGNORE.stream().anyMatch(schema::startsWith)) {
              return null;
            }
            return new NamespaceInfoOracle(schema);
          });
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  /**
   * Converts the JDBC type to the data type of the virtualized database. This method should be as
   * compatible as possible to {@code com.scalar.db.storage.jdbc.RdbEngineOracle} to keep the
   * consistency.
   *
   * <p>Reference: <a
   * href="https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html#GUID-A3C0D836-BADB-44E5-A5D4-265BA5968483">Oracle
   * Database Data Types</a>
   */
  @Override
  public DataType jdbcTypeToDataType(JdbcColumnInfo column) throws UnsupportedJdbcTypeException {
    switch (column.getJdbcType()) {
      case NUMERIC:
        if (column.getSize() > 15) {
          throw new UnsupportedJdbcTypeException(column);
        }
        if (column.getDigits() == 0) {
          return DataType.BigInt.INSTANCE;
        } else {
          return DataType.Double.INSTANCE;
        }
      case FLOAT:
        if (column.getSize() > 53) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Double.INSTANCE;
      case REAL:
        // corresponds to BINARY FLOAT in Oracle
        return DataType.Float.INSTANCE;
      case DOUBLE:
        // corresponds to BINARY DOUBLE in Oracle
        return DataType.Double.INSTANCE;
      case CHAR:
      case NCHAR:
      case VARCHAR:
      case NVARCHAR:
      case CLOB:
      case NCLOB:
        return DataType.Text.INSTANCE;
      case VARBINARY:
      case BLOB:
        return DataType.Blob.INSTANCE;
      case BOOLEAN:
        return DataType.Boolean.INSTANCE;
      case TIMESTAMP:
        if (column.getTypeName().equalsIgnoreCase("DATE")) {
          return DataType.Date.INSTANCE;
        }
        return DataType.TimestampTZ.INSTANCE;
      case OTHER:
        String typeName = column.getTypeName().toUpperCase();
        if (typeName.startsWith("TIMESTAMP") && typeName.endsWith("WITH TIME ZONE")) {
          return DataType.TimestampTZ.INSTANCE;
        } else if (typeName.startsWith("TIMESTAMP") && typeName.endsWith("WITH LOCAL TIME ZONE")) {
          return DataType.Timestamp.INSTANCE;
        }
        throw new UnsupportedJdbcTypeException(column);
      case LONGVARBINARY:
        // LONGVARBINARY correspond to the LONG RAW type in Oracle. LONG RAW requires special care
        // that JDBC clients must use the streaming mode. So, we don't support this type for now.
        // Oracle recommends not using the LONG RAW type.
        // See:
        // https://docs.oracle.com/cd/G11854_01/jjdbc/Java-streams-in-JDBC.html#GUID-E9EBBAB9-BE2D-491F-BB36-A19911B4A9EB
        throw new UnsupportedJdbcTypeException(column);
      case LONGVARCHAR:
        // LONGVARCHAR correspond to the LONG type in Oracle. LONG requires special care the same as
        // LONG RAW. So, we don't support this type for now.
        throw new UnsupportedJdbcTypeException(column);
      default:
        throw new UnsupportedJdbcTypeException(column);
    }
  }
}
