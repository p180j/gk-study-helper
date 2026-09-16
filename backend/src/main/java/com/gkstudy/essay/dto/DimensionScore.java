package com.gkstudy.essay.dto;

import java.math.BigDecimal;

public class DimensionScore {
    private final String code;
    private final String name;
    private final BigDecimal score;

    public DimensionScore(String code, String name, BigDecimal score) {
        this.code = code; this.name = name; this.score = score;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getScore() { return score; }
}
