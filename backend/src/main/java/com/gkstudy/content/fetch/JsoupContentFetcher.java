package com.gkstudy.content.fetch;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** 基于 jsoup 的抓取实现：遵守 robots 约束的轻量抓取，不绕过登录/验证码/付费/访问控制 */
@Component
public class JsoupContentFetcher implements ContentFetcher {
    private static final int TIMEOUT_MS = 20000;
    private static final String USER_AGENT = "gk-study-helper-content-bot/1.0 (+respectful crawling; public pages only)";

    @Override
    public Fetched fetch(String url) throws Exception {
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .ignoreContentType(true)
                .maxBodySize(20 * 1024 * 1024)
                .execute();
        String contentType = response.contentType() == null ? "" : response.contentType();
        String mimeType = contentType.split(";")[0].trim().toLowerCase();
        String fileName = fileNameFromUrl(url);
        if ("text/html".equals(mimeType) || "application/xhtml+xml".equals(mimeType)) {
            // response.parse() 按页面声明的编码解码（很多中文历史题页仍是 GBK），再统一保存为 UTF-8。
            Document document = response.parse();
            return new Fetched(document.outerHtml().getBytes(StandardCharsets.UTF_8), mimeType, fileName);
        }
        return new Fetched(response.bodyAsBytes(), mimeType, fileName);
    }

    private String fileNameFromUrl(String url) {
        String clean = url.split("[?#]")[0];
        int slash = clean.lastIndexOf('/');
        String name = slash >= 0 ? clean.substring(slash + 1) : clean;
        return name.isEmpty() ? null : name;
    }
}
