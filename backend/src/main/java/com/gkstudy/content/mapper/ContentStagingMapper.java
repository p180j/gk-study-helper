package com.gkstudy.content.mapper;

import com.gkstudy.content.model.ContentStaging;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContentStagingMapper {
    @Insert("INSERT INTO content_staging (source_id, source_url, site_name, title, exam_type, source_type, trust_level, parsed_text, status, fail_reason, quality_score, quality_confidence, quality_issues) "
            + "VALUES (#{sourceId}, #{sourceUrl}, #{siteName}, #{title}, #{examType}, #{sourceType}, #{trustLevel}, #{parsedText}, #{status}, #{failReason}, #{qualityScore}, #{qualityConfidence}, #{qualityIssues})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentStaging staging);

    @Select("SELECT * FROM content_staging WHERE id=#{id}")
    ContentStaging findById(Long id);

    @Select("SELECT COUNT(*) FROM content_staging WHERE source_url=#{url}")
    int countByUrl(String url);

    @Select("SELECT COUNT(*) FROM content_staging WHERE file_hash=#{fileHash} AND id<>#{id} AND status IN ('DEDUPED','READY','IMPORTED')")
    int countSameFileHash(@Param("fileHash") String fileHash, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM content_staging WHERE content_hash=#{contentHash} AND id<>#{id} AND status IN ('DEDUPED','READY','IMPORTED')")
    int countSameContentHash(@Param("contentHash") String contentHash, @Param("id") Long id);

    @Select("<script>SELECT s.*, c.name AS source_name FROM content_staging s JOIN content_source c ON c.id=s.source_id "
            + "<where>"
            + "<if test='status != null and status != \"\"'>AND s.status=#{status}</if>"
            + "<if test='sourceId != null'>AND s.source_id=#{sourceId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>AND (s.title LIKE CONCAT('%',#{keyword},'%') OR s.source_url LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "</where> ORDER BY s.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<ContentStaging> list(@Param("status") String status, @Param("sourceId") Long sourceId,
                              @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM content_staging s <where>"
            + "<if test='status != null and status != \"\"'>AND s.status=#{status}</if>"
            + "<if test='sourceId != null'>AND s.source_id=#{sourceId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>AND (s.title LIKE CONCAT('%',#{keyword},'%') OR s.source_url LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "</where></script>")
    int count(@Param("status") String status, @Param("sourceId") Long sourceId, @Param("keyword") String keyword);

    @Select("SELECT * FROM content_staging WHERE source_id=#{sourceId} AND status IN ('DISCOVERED','DOWNLOADED','PARSED','DEDUPED') ORDER BY id")
    List<ContentStaging> findProcessable(Long sourceId);

    @Update("UPDATE content_staging SET status=#{status}, fail_reason=NULL WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE content_staging SET status='FAILED', fail_reason=#{failReason} WHERE id=#{id}")
    int markFailed(@Param("id") Long id, @Param("failReason") String failReason);

    @Update("UPDATE content_staging SET status='NEEDS_REVIEW', fail_reason=#{reason} WHERE id=#{id}")
    int markNeedsReview(@Param("id") Long id, @Param("reason") String reason);

    @Update("UPDATE content_staging SET status='DOWNLOADED', file_hash=#{fileHash}, mime_type=#{mimeType}, file_size=#{fileSize}, "
            + "file_path=#{filePath}, original_file_name=#{originalFileName} WHERE id=#{id}")
    int markDownloaded(ContentStaging staging);

    @Update("UPDATE content_staging SET status='PARSED', parsed_text=#{parsedText}, content_hash=#{contentHash}, "
            + "title=#{title}, publish_org=#{publishOrg}, publish_time=#{publishTime}, source_year=#{sourceYear} WHERE id=#{id}")
    int markParsed(ContentStaging staging);

    @Update("UPDATE content_staging SET status='DEDUPED' WHERE id=#{id}")
    int markDeduped(Long id);

    @Update("UPDATE content_staging SET status='IMPORTED', imported_type=#{importedType}, imported_id=#{importedId}, fail_reason=NULL WHERE id=#{id}")
    int markImported(@Param("id") Long id, @Param("importedType") String importedType, @Param("importedId") Long importedId);

    @Update("UPDATE content_staging SET review_note=#{reviewNote} WHERE id=#{id}")
    int updateReviewNote(@Param("id") Long id, @Param("reviewNote") String reviewNote);
}
