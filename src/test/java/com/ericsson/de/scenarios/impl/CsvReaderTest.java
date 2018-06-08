package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ericsson.de.scenarios.api.DataRecord;

public class CsvReaderTest {

    private static final String CSV = "csv/fromCsvTest.csv";
    private static final String CSV_WRONG_STRUCTURE = "csv/fromCsvTest_WrongCsvStructure.csv";
    private static final String CSV_WITH_QUOTES = "csv/fromCsvTest_WithQuotes.csv";
    private static final String CSV_WITH_QUOTES_INCORRECT_STRUCTURE = "csv/fromCsvTest_WithQuotesIncorrectStructure.csv";
    private static final String CSV_WITH_EMPTY_VALUE = "csv/fromCsvTest_WithEmptyValue.csv";
    private static final String CSV_WITH_EMPTY_FILE = "csv/fromCsvTest_EmptyFile.csv";
    private static final String CSV_WITH_MIX_QUOTED = "csv/fromCsvTest_MixQuoted.csv";
    private static final String CSV_WITH_COMMENTED_ROWS = "csv/fromCsvTest_CommentedOut.csv";
    private static final String CSV_WITH_SPECIAL_CHARACTERS = "csv/fromCsvTest_SpecialCharacters.csv";
    private static final String[] SPECIAL_CHARACTERS = new String[] { "_", " ", "/", "=", ".", "*", "(", ")", ":", ";", "|", "[", "]", "^" };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void read_csvDataRecord() {
        List<DataRecord> dataRecords = CsvReader.read(CSV);
        assertThat(dataRecords.size()).isEqualTo(2);

        Map<String, Object> firstRecord = dataRecords.get(0).getAllFields();
        assertThat(firstRecord.get("username")).isEqualTo("John");
        assertThat(firstRecord.get("password")).isEqualTo("pass");
        assertThat(firstRecord.get("email")).isEqualTo("john.doe@gmail.com");
        assertThat(firstRecord.get("phone")).isEqualTo("7444444");

        Map<String, Object> secondRecord = dataRecords.get(1).getAllFields();
        assertThat(secondRecord.get("username")).isEqualTo("Mike");
        assertThat(secondRecord.get("password")).isEqualTo("qwerty");
        assertThat(secondRecord.get("email")).isEqualTo("mike.qwerty@gmail.com");
        assertThat(secondRecord.get("phone")).isEqualTo("73485753");
    }

    @Test
    public void read_CsvWithSpecialCharacters() {
        List<DataRecord> dataRecords = CsvReader.read(CSV_WITH_SPECIAL_CHARACTERS);
        assertThat(dataRecords).hasSize(2);
        for (DataRecord dataRecord : dataRecords) {
            assertThat(dataRecord.getAllFields()).hasSize(15);
            for (String specialChar : SPECIAL_CHARACTERS) {
                dataRecord.getAllFields().containsValue(specialChar);
            }
        }
    }

    @Test
    public void read_csvWithQuotes() {
        List<DataRecord> dataRecords = CsvReader.read(CSV_WITH_QUOTES);

        Map<String, Object> firstRecord = dataRecords.get(0).getAllFields();
        assertThat(firstRecord.get("username,password")).isEqualTo("John,pass");
        assertThat(firstRecord.get("email,phone")).isEqualTo("john.doe@gmail.com,7444444");

        Map<String, Object> secondRecord = dataRecords.get(1).getAllFields();
        assertThat(secondRecord.get("username,password")).isEqualTo("Mike,qwerty");
        assertThat(secondRecord.get("email,phone")).isEqualTo("mike.qwerty@gmail.com,73485753");
    }

    @Test
    public void read_csvWithEmptyValue() {
        List<DataRecord> dataRecords = CsvReader.read(CSV_WITH_EMPTY_VALUE);

        Map<String, Object> firstRecord = dataRecords.get(0).getAllFields();
        assertThat(firstRecord.get("username")).isEqualTo("");
        assertThat(firstRecord.get("password")).isEqualTo("");
        assertThat(firstRecord.get("email")).isEqualTo("");
        assertThat(firstRecord.get("phone")).isEqualTo("");

        Map<String, Object> secondRecord = dataRecords.get(1).getAllFields();
        assertThat(secondRecord.get("username")).isEqualTo("");
        assertThat(secondRecord.get("password")).isEqualTo("");
        assertThat(secondRecord.get("email")).isEqualTo("mike.qwerty@gmail.com");
        assertThat(secondRecord.get("phone")).isEqualTo("");
    }

    @Test
    public void read_mixQuoted() {
        List<DataRecord> dataRecords = CsvReader.read(CSV_WITH_MIX_QUOTED);

        Map<String, Object> firstRecord = dataRecords.get(0).getAllFields();
        assertThat(firstRecord.get("username")).isEqualTo("John,pass");
        assertThat(firstRecord.get("password")).isEqualTo("john.doe@gmail.com");
        assertThat(firstRecord.get("email,phone")).isEqualTo("7444444");

        Map<String, Object> secondRecord = dataRecords.get(1).getAllFields();
        assertThat(secondRecord.get("username")).isEqualTo("Mike");
        assertThat(secondRecord.get("password")).isEqualTo("qwerty");
        assertThat(secondRecord.get("email,phone")).isEqualTo("mike.qwerty@gmail.com,73485753");
    }

    @Test
    public void read_commentedOutRows() {
        List<DataRecord> dataRecords = CsvReader.read(CSV_WITH_COMMENTED_ROWS);
        assertThat(dataRecords).hasSize(1);
    }

    @Test
    public void read_nullPath() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(CsvReader.ERROR_PATH_UNDEFINED);

        CsvReader.read(null);
    }

    @Test
    public void read_throwExceptionForFilesWithWrongFileExtension() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(CsvReader.ERROR_CSV_EXTENSION);

        CsvReader.read("fromCsvTest.txt");
    }

    @Test
    public void read_resourceDoesNotExist() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("resource nonExistingCsvFile.csv not found.");

        CsvReader.read("nonExistingCsvFile.csv");
    }

    @Test
    public void read_throwsExceptionForCsvWithIncorrectStructure() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(String.format(CsvReader.ERROR_COLUMN_MISMATCH_TEMPLATE, "4", "2"));

        CsvReader.read(CSV_WRONG_STRUCTURE);
    }

    @Test
    public void read_csvWithQuotesIncorrectStructure() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(String.format(CsvReader.ERROR_COLUMN_MISMATCH_TEMPLATE, "3", "2"));

        CsvReader.read(CSV_WITH_QUOTES_INCORRECT_STRUCTURE);
    }

    @Test
    public void read_emptyCsv() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(CsvReader.ERROR_DATA_RECORD_LIST_EMPTY);
        CsvReader.read(CSV_WITH_EMPTY_FILE);
    }
}
