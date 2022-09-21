package com.dremio.exec.store.jdbc.rel2sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexProgram;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlWindow;
import org.apache.calcite.sql.fun.SqlSingleValueAggFunction;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.InferTypes;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidatorUtil;

import com.dremio.exec.store.jdbc.ColumnPropertiesProcessors;
import com.dremio.exec.store.jdbc.dialect.JdbcDremioSqlDialect;
import com.dremio.exec.store.jdbc.rel.JdbcSort;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * SQL Server-specific query translator.
 */
public class BigQueryRelToSqlConverter extends JdbcDremioRelToSqlConverter {

  public BigQueryRelToSqlConverter(JdbcDremioSqlDialect dialect) {
    super(dialect);
  }

  @Override
  protected JdbcDremioRelToSqlConverter getJdbcDremioRelToSqlConverter() {
    return this;
  }

  // Calcite overrides
  @Override
  public void addSelect(List<SqlNode> selectList, SqlNode node, RelDataType rowType) {
    final String name = rowType.getFieldNames().get(selectList.size());
    final String alias = SqlValidatorUtil.getAlias(node, -1);
    if (alias == null || !alias.equals(name)) {
	  if ("$_dremio_$_update_$".equalsIgnoreCase(name)) {
        final String newname = "_dremio__update_";
		node = SqlStdOperatorTable.AS.createCall(POS, node, new SqlIdentifier(newname, POS));
	  } else if (name.contains("$")) {
		final String newname = name.replaceAll("\\$", "_dremio__");
		node = SqlStdOperatorTable.AS.createCall(POS, node, new SqlIdentifier(newname, POS));
	  } else {
		node = SqlStdOperatorTable.AS.createCall(POS, node, new SqlIdentifier(name, POS));
	  }
    }
    selectList.add(node);
  }
}