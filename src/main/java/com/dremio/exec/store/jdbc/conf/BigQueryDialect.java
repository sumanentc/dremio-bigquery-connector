package com.dremio.exec.store.jdbc.conf;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.dremio.exec.store.jdbc.JdbcPluginConfig;
import com.dremio.exec.store.jdbc.JdbcSchemaFetcherImpl;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.dremio.exec.store.jdbc.dialect.arp.ArpTypeMapper;
import com.dremio.exec.store.jdbc.dialect.arp.ArpYaml;
import com.dremio.exec.store.jdbc.dialect.SourceTypeDescriptor;
import com.dremio.exec.store.jdbc.dialect.TypeMapper;
import com.dremio.exec.store.jdbc.rel2sql.BigQueryRelToSqlConverter;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import com.google.common.base.Preconditions;

/**
 * Custom Dialect for BigQuery.
 * See Apache Calcite project for Dialect info
 * https://github.com/apache/calcite/blob/4208d0ba6f2a749692fe64181a1373af07d55db5/core/src/main/java/org/apache/calcite/sql/dialect/BigQuerySqlDialect.java
 */
public class BigQueryDialect extends ArpDialect {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BigQueryDialect.class);
  private final ArpTypeMapper typeMapper;
  
  public BigQueryDialect(final ArpYaml yaml) {
    super(yaml);
	typeMapper = new BigQueryTypeMapper(yaml);
  }

  /**
   * Custom Dialect for BigQuery.
   * The following block is required as BigQuery reports integers as NUMBER(38,0).
   */
  static class BigQuerySchemaFetcher extends JdbcSchemaFetcherImpl {
    public BigQuerySchemaFetcher(JdbcPluginConfig config) {
      super(config);
    }

    @Override
    protected boolean usePrepareForColumnMetadata() {
      return true;
    }
  }

  // Dremio-specific
  @Override
  public BigQueryRelToSqlConverter getConverter() {
    return new BigQueryRelToSqlConverter(this);
  }
  
  @Override
  public JdbcSchemaFetcherImpl newSchemaFetcher(JdbcPluginConfig config) {
    return new BigQuerySchemaFetcher(config);
  }

  @Override
  public boolean supportsNestedAggregations() {
    return false;
  }

  @Override
  public void unparseCall(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
    if (call instanceof SqlSelect) {
      SqlSelect select = (SqlSelect) call;
      SqlNodeList group = select.getGroup();

      if (group != null) {
        for (int i = 0; i < group.size(); i++) {
          SqlNode sqlNode = group.get(i);
          if (sqlNode instanceof SqlLiteral) {
            SqlLiteral stringLiteral = (SqlLiteral) sqlNode;
            group.set(i, new SqlBasicCall(SqlStdOperatorTable.COALESCE, new SqlNode[]{stringLiteral}, SqlParserPos.ZERO));
          }
        }
      }
    }
    super.unparseCall(writer, call, leftPrec, rightPrec);
  }
  
  @Override
  public TypeMapper getDataTypeMapper(JdbcPluginConfig config) {
    return this.typeMapper;
  }

  private static class BigQueryTypeMapper extends ArpTypeMapper {
    public BigQueryTypeMapper(ArpYaml yaml) {
      super(yaml);
    }
	
	@Override
    protected SourceTypeDescriptor createTypeDescriptor(
      AddPropertyCallback addColumnPropertyCallback, InvalidMetaDataCallback invalidMetaDataCallback,
      Connection connection, TableIdentifier table, ResultSetMetaData metaData, String columnLabel, int colIndex)
      throws SQLException {
      Preconditions.checkNotNull(invalidMetaDataCallback);
      if ("_dremio__update_".equalsIgnoreCase(columnLabel)) {
		  columnLabel = "$_dremio_$_update_$";
	  } else if (columnLabel.contains("_dremio__")) {
		  columnLabel = columnLabel.replaceAll("_dremio__", "\\$");
	  }

      return super.createTypeDescriptor(addColumnPropertyCallback, invalidMetaDataCallback, connection, table, metaData, columnLabel, colIndex);
    }
  }
}
