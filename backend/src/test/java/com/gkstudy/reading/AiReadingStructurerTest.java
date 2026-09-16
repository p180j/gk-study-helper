package com.gkstudy.reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import com.gkstudy.reading.ai.AiReadingStructurer;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.reading.service.AdminReadingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiReadingStructurerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void structuresPoliticalMaterialAndKeepsLowConfidenceDraft() throws Exception {
        String json = "{\"coreView\":\"治理重心下移\",\"problem\":\"资源不足\",\"cause\":\"协同不够\",\"solution\":\"资源下沉\",\"policyLogic\":\"问题到对策\",\"standardExpressions\":[\"资源下沉\"],\"cases\":[\"网格治理\"],\"applicableEssayThemes\":[\"基层治理\"],\"topicCandidates\":[\"基层治理\"],\"confidence\":55}";
        AiReadingStructurer structurer = new AiReadingStructurer(provider(json), mapper);
        ReadingMaterialMapper materialMapper = mock(ReadingMaterialMapper.class);
        ReadingMaterial material = material();
        when(materialMapper.findById(9L)).thenReturn(material);
        AdminReadingService service = new AdminReadingService(mock(PoliticalTopicMapper.class), materialMapper,
                mock(KnowledgePointMapper.class), mapper, structurer);

        service.aiStructure(9L);

        ArgumentCaptor<ReadingMaterial> captor = ArgumentCaptor.forClass(ReadingMaterial.class);
        verify(materialMapper).updateAiStructure(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getAiStatus());
        assertEquals(0, captor.getValue().getAiConfidence().compareTo(new java.math.BigDecimal("55.00")));
        assertEquals("治理重心下移", captor.getValue().getCoreView());
        assertEquals("DRAFT", captor.getValue().getStatus());
    }

    @Test
    void rejectsMissingStructuredField() throws Exception {
        AiReadingStructurer structurer = new AiReadingStructurer(provider("{\"confidence\":50}"), mapper);
        assertThrows(RuntimeException.class, () -> structurer.structure(material()));
    }

    private AiProvider provider(String json) throws Exception {
        AiResponse response = new AiResponse("OPENAI_COMPATIBLE", "model-x", mapper.readTree(json), "raw",
                LocalDateTime.now(), 8);
        return new AiProvider() {
            public AiResponse completeStructured(String system, String user, int max) { return response; }
            public boolean healthCheck() { return true; }
        };
    }

    private ReadingMaterial material() {
        ReadingMaterial material = new ReadingMaterial(); material.setId(9L); material.setTitle("基层治理");
        material.setContent("推动治理重心下移，促进资源下沉。"); material.setStatus("PUBLISHED"); return material;
    }
}
