package com.ericsson.de.scenarios.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

final class CsvReader {

    private static final String CSV_EXTENSION = ".csv";

    static final String ERROR_PATH_UNDEFINED = "'path' is undefined";
    static final String ERROR_CSV_EXTENSION = String.format("File extension must be '%s'", CSV_EXTENSION);
    static final String ERROR_DATA_RECORD_LIST_EMPTY = "Data source is empty";
    static final String ERROR_COLUMN_MISMATCH_TEMPLATE = "Amount of headers and values must be equal. Headers %s; Values: %s";

    private CsvReader() {
    }

    static List<DataRecord> read(String path) {
        Preconditions.checkNotNull(path, ERROR_PATH_UNDEFINED);
        Preconditions.checkState(path.endsWith(CSV_EXTENSION), ERROR_CSV_EXTENSION);

        List<DataRecord> dataRecordList;
        try (BufferedReader reader = createReader(path)) {
            Iterator<CSVRecord> records = CSVFormat.EXCEL.withCommentMarker('#').parse(reader).iterator();
            dataRecordList = parseCSV(records);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }

        Preconditions.checkState(!dataRecordList.isEmpty(), ERROR_DATA_RECORD_LIST_EMPTY);
        return dataRecordList;
    }

    private static List<DataRecord> parseCSV(final Iterator<CSVRecord> records) {
        List<DataRecord> dataRecordList = Lists.newArrayList();
        if (records.hasNext()) {
            List<String> headers = parseLine(records.next());
            while (records.hasNext()) {
                List<String> cells = parseLine(records.next());
                dataRecordList.add(toDataRecord(headers, cells));
            }
        }
        return dataRecordList;
    }

    private static List<String> parseLine(CSVRecord csvRecord) {
        List<String> cells = new ArrayList<>();
        for (String cell : csvRecord) {
            cells.add(cell);
        }
        return cells;
    }

    private static BufferedReader createReader(String path) throws FileNotFoundException {
        FileReader fileReader = new FileReader(Resources.getResource(path).getFile());
        return new BufferedReader(fileReader);
    }

    private static DataRecord toDataRecord(List<String> headers, List<String> values) {
        Preconditions.checkState(headers.size() == values.size(), ERROR_COLUMN_MISMATCH_TEMPLATE, headers.size(), values.size());
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), values.get(i));
        }
        return BasicDataRecord.fromMap(row);
    }
}
