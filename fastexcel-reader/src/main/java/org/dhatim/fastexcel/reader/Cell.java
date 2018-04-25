package org.dhatim.fastexcel.reader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Cell {

    private static final long DAY_MILLISECONDS = 86_400_000L;

    private final ReadableWorkbook workbook;
    private final Object value;
    private final String formula;
    private final CellType type;
    private final CellAddress address;
    private final String rawValue;

    public Cell(ReadableWorkbook workbook, CellType type, Object value, CellAddress address, String formula, String rawValue) {
        this.workbook = workbook;
        this.type = type;
        this.value = value;
        this.address = address;
        this.formula = formula;
        this.rawValue = rawValue;
    }

    public CellType getType() {
        return type;
    }

    public int getColumnIndex() {
        return address.getColumn();
    }

    public CellAddress getAddress() {
        return address;
    }

    public Object getValue() {
        return value;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getFormula() {
        return formula;
    }

    public BigDecimal asNumber() {
        requireType(CellType.NUMBER);
        return (BigDecimal) value;
    }

    public OffsetDateTime asDate() {
        if (type == CellType.DATE) {
            return (OffsetDateTime) value;
        } else if (type == CellType.NUMBER) {
            return convertToDate(Double.parseDouble(rawValue));
        } else if (type == CellType.EMPTY) {
            return null;
        } else {
            throw new ExcelReaderException("Wrong cell type " + type + " for date value");
        }
    }

    private OffsetDateTime convertToDate(double value) {
        int wholeDays = (int) Math.floor(value);
        long millisecondsInDay = (long) (((value - wholeDays) * DAY_MILLISECONDS) + 0.5D);

        int startYear = 1900;
        int dayAdjust = -1; // Excel thinks 2/29/1900 is a valid date, which it isn't
        if (workbook.isDate1904()) {
            startYear = 1904;
            dayAdjust = 1; // 1904 date windowing uses 1/2/1904 as the first day
        } else if (wholeDays < 61) {
            // Date is prior to 3/1/1900, so adjust because Excel thinks 2/29/1900 exists
            // If Excel date == 2/29/1900, will become 3/1/1900 in Java representation
            dayAdjust = 0;
        }
        LocalDate localDate = LocalDate.of(startYear, 1, 1).plusDays((long) wholeDays + dayAdjust - 1);
        LocalTime localTime = LocalTime.ofNanoOfDay(millisecondsInDay * 1_000_000);
        ZonedDateTime date = ZonedDateTime.of(localDate, localTime, ZoneId.systemDefault());
        return date.toOffsetDateTime();
    }

    public boolean asBoolean() {
        requireType(CellType.BOOLEAN);
        return (Boolean) value;
    }

    public String asString() {
        requireType(CellType.STRING);
        return value == null ? "" : (String) value;
    }

    private void requireType(CellType requiredType) {
        if (type != requiredType && type != CellType.EMPTY) {
            throw new ExcelReaderException("Wrong cell type " + type + ", wanted " + requiredType);
        }
    }

    public String getText() {
        return value == null ? "" : value.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(type).append(' ');
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(value).append('"');
        }
        return sb.append(']').toString();
    }

}