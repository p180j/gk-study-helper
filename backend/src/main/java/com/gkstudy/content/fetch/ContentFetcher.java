package com.gkstudy.content.fetch;

/** 内容抓取端口：隔离网络访问，便于测试 */
public interface ContentFetcher {
    Fetched fetch(String url) throws Exception;

    class Fetched {
        public final byte[] body;
        public final String mimeType;
        public final String fileName;

        public Fetched(byte[] body, String mimeType, String fileName) {
            this.body = body; this.mimeType = mimeType; this.fileName = fileName;
        }
    }
}
