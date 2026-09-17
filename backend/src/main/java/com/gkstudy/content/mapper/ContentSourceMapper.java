package com.gkstudy.content.mapper;

import com.gkstudy.content.model.ContentSource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContentSourceMapper {
    @Select("SELECT * FROM content_source ORDER BY id")
    List<ContentSource> findAll();

    @Select("SELECT * FROM content_source WHERE id=#{id}")
    ContentSource findById(Long id);

    @Insert("INSERT INTO content_source (name, base_url, source_type, exam_type, trust_level, enabled, crawl_strategy, status) "
            + "VALUES (#{name}, #{baseUrl}, #{sourceType}, #{examType}, #{trustLevel}, #{enabled}, #{crawlStrategy}, 'IDLE')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentSource source);

    @Update("UPDATE content_source SET name=#{name}, base_url=#{baseUrl}, source_type=#{sourceType}, exam_type=#{examType}, "
            + "trust_level=#{trustLevel}, enabled=#{enabled}, crawl_strategy=#{crawlStrategy} WHERE id=#{id}")
    int update(ContentSource source);

    @Update("UPDATE content_source SET enabled=#{enabled} WHERE id=#{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    @Update("UPDATE content_source SET last_crawl_time=NOW(), status=#{status} WHERE id=#{id}")
    int updateCrawlState(@Param("id") Long id, @Param("status") String status);
}
