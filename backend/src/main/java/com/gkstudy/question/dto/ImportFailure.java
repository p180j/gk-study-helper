package com.gkstudy.question.dto;

public class ImportFailure {
    private final int row;
    private final String reason;

    public ImportFailure(int row, String reason) { this.row = row; this.reason = reason; }
    public int getRow() { return row; }
    public String getReason() { return reason; }
}
