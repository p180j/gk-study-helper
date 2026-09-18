package com.gkstudy.content.dto;

import java.util.ArrayList;
import java.util.List;

/** 内容库存聚合响应：总览计数 + 一级模块（含子知识点）库存 */
public class InventoryOverviewView {
    private Overview overview = new Overview();
    private List<ModuleView> modules = new ArrayList<>();

    public static class Overview {
        private int trainableTotal;
        private int mockReservedTotal;
        private int essayTotal;
        private int aiTotal;
        private int needsReviewCount;
        private int crawlFailedCount;

        public int getTrainableTotal() { return trainableTotal; }
        public void setTrainableTotal(int trainableTotal) { this.trainableTotal = trainableTotal; }
        public int getMockReservedTotal() { return mockReservedTotal; }
        public void setMockReservedTotal(int mockReservedTotal) { this.mockReservedTotal = mockReservedTotal; }
        public int getEssayTotal() { return essayTotal; }
        public void setEssayTotal(int essayTotal) { this.essayTotal = essayTotal; }
        public int getAiTotal() { return aiTotal; }
        public void setAiTotal(int aiTotal) { this.aiTotal = aiTotal; }
        public int getNeedsReviewCount() { return needsReviewCount; }
        public void setNeedsReviewCount(int needsReviewCount) { this.needsReviewCount = needsReviewCount; }
        public int getCrawlFailedCount() { return crawlFailedCount; }
        public void setCrawlFailedCount(int crawlFailedCount) { this.crawlFailedCount = crawlFailedCount; }
    }

    public static class ModuleView {
        private Long id;
        private String code;
        private String moduleName;
        private int totalQuestions;
        private int trainable;
        private int unused;
        private List<PointView> knowledgePoints = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public int getTrainable() { return trainable; }
        public void setTrainable(int trainable) { this.trainable = trainable; }
        public int getUnused() { return unused; }
        public void setUnused(int unused) { this.unused = unused; }
        public List<PointView> getKnowledgePoints() { return knowledgePoints; }
        public void setKnowledgePoints(List<PointView> knowledgePoints) { this.knowledgePoints = knowledgePoints; }
    }

    public static class PointView {
        private Long id;
        private String code;
        private String name;
        private int totalQuestions;
        private int trainable;
        private int unused;
        private boolean lowStock;

        public PointView(InventoryItem item, boolean lowStock) {
            this.id = item.getId(); this.code = item.getCode(); this.name = item.getName();
            this.totalQuestions = item.getTotalQuestions(); this.trainable = item.getTrainable();
            this.unused = item.getUnused(); this.lowStock = lowStock;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public int getTotalQuestions() { return totalQuestions; }
        public int getTrainable() { return trainable; }
        public int getUnused() { return unused; }
        public boolean isLowStock() { return lowStock; }
    }

    public Overview getOverview() { return overview; }
    public void setOverview(Overview overview) { this.overview = overview; }
    public List<ModuleView> getModules() { return modules; }
    public void setModules(List<ModuleView> modules) { this.modules = modules; }
}
