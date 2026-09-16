package com.gkstudy.essay.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class ReferencePoint {
    @NotBlank private String point;
    private List<String> keywords;

    public String getPoint() { return point; }
    public void setPoint(String point) { this.point = point; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
}
