package com.videoamp.cleanroom.queryanalyzer;

import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.*;
import com.google.zetasql.*;
import com.google.zetasql.resolvedast.ResolvedNodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

class BigQueryCatalog extends SimpleCatalog {
    private BigQuery bigquery;

    BigQueryCatalog(String name) {
        super(name);
        bigquery = BigQueryOptions.getDefaultInstance().getService();
    }

    SimpleCatalog getOrCreateCatalogForTable(String[] tableNameSegments) {
        return getOrCreateCatalogForTable(this, tableNameSegments);
    }

    private static SimpleCatalog getOrCreateCatalogForTable(SimpleCatalog catalog, String[] tableNameSegments) {
        if (tableNameSegments.length == 1) {
            return catalog;
        } else {
            String catalogName = tableNameSegments[0];
            SimpleCatalog nextCatalog = catalog.getCatalog(catalogName, null);
            if (nextCatalog == null) {
                nextCatalog = catalog.addNewSimpleCatalog(catalogName);
            }
            return getOrCreateCatalogForTable(
                    nextCatalog,
                    Arrays.copyOfRange(tableNameSegments, 1, tableNameSegments.length)
            );
        }
    }

    SimpleTable lookupTable(String[] tableNameSegments) {
        TableId tableId;
        if (tableNameSegments.length == 3) {
            tableId = TableId.of(tableNameSegments[0], tableNameSegments[1], tableNameSegments[2]);
        } else {
            tableId = TableId.of(tableNameSegments[0], tableNameSegments[1]);
        }

        Table table = bigquery.getTable(tableId);
        SimpleTable simpleTable = new SimpleTable(tableId.getTable());
        for (Field field : Objects.requireNonNull(table.getDefinition().getSchema()).getFields()) {
            String typeName = field.getType().getStandardType().name();
            if (typeName.equals("FLOAT64")) {
                typeName = "FLOAT";
            }
            Type fieldType = TypeFactory.createSimpleType(
                    ZetaSQLType.TypeKind.valueOf(
                            "TYPE_" + typeName
                    )
            );
            simpleTable.addSimpleColumn(field.getName(), fieldType);
        }
        return simpleTable;
    }

    void addFunction(ResolvedNodes.ResolvedCreateFunctionStmt resolvedStatement) {
        addFunction(
            new Function(
                    resolvedStatement.getNamePath(),
                    "SQL_Function",
                    ZetaSQLFunctions.FunctionEnums.Mode.SCALAR,
                    Collections.singletonList(resolvedStatement.getSignature())
            )
        );
    }
}
