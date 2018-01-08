/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.data_log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author victor
 */
public class DataLog {

    private static final Logger LOG = Logger.getLogger(DataLog.class.getName());
    
    public static DataLog loadFromXLSX(String filePath, 
            String sheetName, 
            String timestampField,
            String varnameField,
            String dataField) throws FileNotFoundException, IOException {
        
        File logFile = new File(filePath);
        FileInputStream fis = new FileInputStream(logFile);
        
        // Finds the workbook instance for XLSX file
        XSSFWorkbook wb = new XSSFWorkbook(fis);
        XSSFSheet sheet = wb.getSheet(sheetName);
        
        //Get the column indexes of the timestamp, varname and data
        Map<String,Integer> columnIndexes = new HashMap<>();
            columnIndexes.put(timestampField, -1);
            columnIndexes.put(varnameField, -1);
            columnIndexes.put(dataField, -1);
            
        for (Cell cell : sheet.getRow(0)) {
            String fieldName = cell.getStringCellValue();
            if (columnIndexes.containsKey(fieldName)) {
                if (columnIndexes.get(fieldName) == -1)
                    columnIndexes.put(fieldName, cell.getColumnIndex());
                else
                    throw new IOException("Duplicated field name: " + fieldName);
            }
        }
        
        //Check if all the fields have been associated to a column index
        if (columnIndexes.values().stream().anyMatch(index -> (index == -1))) {
            throw new IOException("Some necessary columns are missing");
        }
        
        //Read data
        Collection<LogEntry> entries = new HashSet<>();
        for (Row row : sheet) {
            if (row.getRowNum() > 0) {
                //Data
                Cell dataCell = row.getCell(columnIndexes.get(dataField));
                Object dataValue = null;
                switch (dataCell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        String dataValueText = dataCell.getStringCellValue();
                        //Check if the string can be read as a number
                        try {
                            dataValue = Integer.valueOf(dataValueText);
                        } catch (NumberFormatException e2) {
                            try {
                                dataValue = Double.valueOf(dataValueText);
                            } catch (NumberFormatException e3) {
                                dataValue = dataValueText;
                            }
                        }
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        dataValue = dataCell.getNumericCellValue();
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        dataValue = dataCell.getBooleanCellValue();
                        break;
                    default:
                        throw new IOException("Not recognized cell type at row " + 
                                (row.getRowNum()+1) + ", column " + dataField);
                }
                
                entries.add(new LogEntry(
                        new BigDecimal(
                                String.valueOf(
                                        row.getCell(
                                                columnIndexes.get(
                                                        timestampField
                                                )
                                        ).getNumericCellValue()
                                )
                        ).longValue(),
                        row.getCell(
                                columnIndexes.get(varnameField)
                        ).getStringCellValue(),
                        dataValue)
                );
            }
        }
        
        return new DataLog(filePath, entries);
    }

    public DataLog(String id) {
        this.id = id;
    }

    public DataLog(String id, Collection<LogEntry> entries) {
        this.id = id;
        this.entries = entries;
    }
    
    public Optional<LogEntry> logState(Long ts, String varname) {
        return closestPastLogEntry(this.getEntries(),ts, varname);
    }
    
    public Collection<LogEntry> logState(Long ts) {
        return  this.getEntries().stream()
                .map(LogEntry::getVarname)
                .distinct()
                .map((String v) -> this.logState(ts, v))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }
    
    public Collection<LogEntry> logState(Long startTime, 
                                            Long endTime, 
                                            String varname) {
        
        List<Long> record_times = new ArrayList<>(Arrays.asList(startTime));
        record_times.addAll(
            this.getEntries().stream()
            .filter(e -> e.getTs() > startTime)
            .filter(e -> e.getTs() <= (endTime))
            .filter(e -> e.getVarname().equals(varname))
            .map(LogEntry::getTs)
            .distinct()
            .sorted()
            .collect(Collectors.toList())
        );
        
        return  record_times.stream()
                .map((record_time) -> {
                    return logState(record_time, varname);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toSet());
    }
    
    public Collection<LogEntry> logState(Long startTime, Long endTime) {
        return  this.getEntries().stream()
                .map(LogEntry::getVarname)
                .distinct()
                .map((String v) -> this.logState(startTime, endTime, v))
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
    }
    
    
    /**
     * Getters & Setters
     */

    public String getId() {
        return id;
    }

    public Collection<LogEntry> getEntries() {
        return entries;
    }
    
    /**
     * Private methods
     */
    private Optional<LogEntry> closestPastLogEntry(Collection<LogEntry> logEntries,
                                            Long ts, 
                                            String varname) {
        return logEntries.stream()
                .filter(e -> e.getVarname().equals(varname))
                .filter(e -> e.getTs() <= ts)
                .max((LogEntry e1, LogEntry e2) -> {
                    return (int) (e1.getTs() - e2.getTs());
                });
    }
    
    /**
     * Attributes
     */
    private String id;
    private Collection<LogEntry> entries;
}
