/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2tools.jaqu;

/**
 * This class represents a column of a table in a query.
 *
 * @param <T> the table data type
 */
class SelectColumn<T> {
    private final SelectTable<T> selectTable;
    private final TableDefinition.FieldDefinition fieldDef;

    SelectColumn(SelectTable<T> table, TableDefinition.FieldDefinition fieldDef) {
        this.selectTable = table;
        this.fieldDef = fieldDef;
    }

    void appendSQL(SQLStatement stat) {
        if (selectTable.getQuery().isJoin()) {
            stat.appendSQL(selectTable.getAs() + "." + fieldDef.columnName);
        } else {
            stat.appendSQL(fieldDef.columnName);
        }
    }

    TableDefinition.FieldDefinition getFieldDefinition() {
        return fieldDef;
    }

    SelectTable<T> getSelectTable() {
        return selectTable;
    }

    Object getCurrentValue() {
        return fieldDef.getValue(selectTable.getCurrent());
    }
}
