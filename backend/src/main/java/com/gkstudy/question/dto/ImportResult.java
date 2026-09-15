package com.gkstudy.question.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int successCount;
    private int failureCount;
    private final List<ImportFailure> failures = new ArrayList<>();

    public void success() { successCount++; }
    public void failure(int row, String reason) { failureCount++; failures.add(new ImportFailure(row, reason)); }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public List<ImportFailure> getFailures() { return failures; }
}
