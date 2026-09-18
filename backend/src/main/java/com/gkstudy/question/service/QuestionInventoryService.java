package com.gkstudy.question.service;

import com.gkstudy.question.mapper.QuestionInventoryMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/** 题库库存服务：为计划生成与练习进度提供可用题量、未做题量与模块库存汇总。 */
@Service
public class QuestionInventoryService {
    private final QuestionInventoryMapper inventoryMapper;
    @Value("${question.low-stock-threshold:10}")
    private int lowStockThreshold;

    public QuestionInventoryService(QuestionInventoryMapper inventoryMapper) { this.inventoryMapper = inventoryMapper; }

    /** 知识点及其同父模块兄弟知识点范围内可用去重题目总数（不含 MOCK_RESERVED，VALIDATION 额外排除 C/D 级题源）。 */
    public int countAvailable(Long knowledgePointId, String purpose, Long userId) {
        return inventoryMapper.countAvailable(knowledgePointId, purpose);
    }

    /** 同 countAvailable 范围，但仅统计该用户未做过的题。 */
    public int countUnused(Long knowledgePointId, String purpose, Long userId) {
        return inventoryMapper.countUnused(knowledgePointId, purpose, userId);
    }

    /** 全部一级模块的可用未做题库存汇总。 */
    public List<ModuleInventory> moduleSummary(Long userId) { return inventoryMapper.moduleSummary(userId); }

    /** 全部普通可训练题库存，供管理后台总览复用计划同一套可用题定义。 */
    public int totalAvailable() { return inventoryMapper.totalAvailable(); }

    public int getLowStockThreshold() { return lowStockThreshold; }

    public static class ModuleInventory {
        private String moduleCode;
        private String moduleName;
        private Integer availableCount;

        public String getModuleCode() { return moduleCode; }
        public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }
        public Integer getAvailableCount() { return availableCount; }
        public void setAvailableCount(Integer availableCount) { this.availableCount = availableCount; }
    }
}
