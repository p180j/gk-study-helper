package com.gkstudy.ai.mapper;

import com.gkstudy.ai.model.AiProviderConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiProviderConfigMapper {
    @Select("SELECT id,provider_code,enabled,model,custom_model,base_url,api_key_cipher,masked_key,is_default AS default_provider,last_test_status,last_test_message,last_test_time,last_test_latency_ms,last_test_model,create_time,update_time FROM ai_provider_config ORDER BY id")
    List<AiProviderConfig> findAll();

    @Select("SELECT id,provider_code,enabled,model,custom_model,base_url,api_key_cipher,masked_key,is_default AS default_provider,last_test_status,last_test_message,last_test_time,last_test_latency_ms,last_test_model,create_time,update_time FROM ai_provider_config WHERE provider_code=#{code}")
    AiProviderConfig findByCode(String code);

    @Select("SELECT id,provider_code,enabled,model,custom_model,base_url,api_key_cipher,masked_key,is_default AS default_provider,last_test_status,last_test_message,last_test_time,last_test_latency_ms,last_test_model,create_time,update_time FROM ai_provider_config WHERE is_default=1 AND enabled=1")
    List<AiProviderConfig> findDefaultEnabled();

    @Insert("INSERT INTO ai_provider_config (provider_code, enabled, model, custom_model, base_url, api_key_cipher, masked_key, is_default) "
            + "VALUES (#{providerCode}, #{enabled}, #{model}, #{customModel}, #{baseUrl}, #{apiKeyCipher}, #{maskedKey}, #{defaultProvider})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiProviderConfig config);

    @Update("UPDATE ai_provider_config SET enabled=#{enabled}, model=#{model}, custom_model=#{customModel}, base_url=#{baseUrl}, "
            + "api_key_cipher=#{apiKeyCipher}, masked_key=#{maskedKey} WHERE provider_code=#{providerCode}")
    int update(AiProviderConfig config);

    @Update("UPDATE ai_provider_config SET is_default=CASE WHEN provider_code=#{code} THEN 1 ELSE 0 END")
    int switchDefault(String code);

    @Update("UPDATE ai_provider_config SET last_test_status=#{status}, last_test_message=#{message}, last_test_time=NOW(), "
            + "last_test_latency_ms=#{latencyMs}, last_test_model=#{testedModel} WHERE provider_code=#{code}")
    int updateTestResult(String code, String status, String message, Integer latencyMs, String testedModel);
}
