package com.gkstudy.content.mapper;

import com.gkstudy.content.model.ContentCrawlLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContentCrawlLogMapper {
    @Insert("INSERT INTO content_crawl_log (source_id, source_name, status, start_time) "
            + "VALUES (#{sourceId}, #{sourceName}, #{status}, #{startTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentCrawlLog entry);

    @Update("UPDATE content_crawl_log SET status=#{status}, discovered=#{discovered}, downloaded=#{downloaded}, "
            + "parsed=#{parsed}, imported=#{imported}, duplicates=#{duplicates}, needs_review=#{needsReview}, "
            + "failed=#{failed}, message=#{message}, end_time=#{endTime} WHERE id=#{id}")
    int update(ContentCrawlLog entry);

    @Select("SELECT * FROM content_crawl_log WHERE source_id=#{sourceId} ORDER BY id DESC LIMIT 1")
    ContentCrawlLog findLatestBySource(Long sourceId);

    @Select("<script>SELECT * FROM content_crawl_log "
            + "<where><if test='sourceId != null'>AND source_id=#{sourceId}</if></where> "
            + "ORDER BY id DESC LIMIT #{limit}</script>")
    List<ContentCrawlLog> findList(@Param("sourceId") Long sourceId, @Param("limit") int limit);
}
