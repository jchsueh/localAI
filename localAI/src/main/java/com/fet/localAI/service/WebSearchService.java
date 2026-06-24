package com.fet.localAI.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebSearchService {
    
    private static final String GOOGLE_SEARCH_URL = "https://www.google.com/search?q=";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    /**
     * 執行網頁搜尋並返回結果
     */
    public List<Map<String, String>> search(String query, int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = GOOGLE_SEARCH_URL + encodedQuery + "&num=" + maxResults;
            
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();
            
            // 解析搜尋結果
            Elements searchResults = doc.select("div.g");
            
            for (Element result : searchResults) {
                if (results.size() >= maxResults) break;
                
                // 提取標題
                Element titleElement = result.selectFirst("h3");
                String title = titleElement != null ? titleElement.text() : "";
                
                // 提取連結
                Element linkElement = result.selectFirst("a[href]");
                String link = linkElement != null ? linkElement.attr("href") : "";
                
                // 提取摘要
                Element snippetElement = result.selectFirst("div[data-sncf], div.VwiC3b, span.aCOpRe");
                String snippet = snippetElement != null ? snippetElement.text() : "";
                
                if (!title.isEmpty() && !link.isEmpty()) {
                    Map<String, String> searchResult = new HashMap<>();
                    searchResult.put("title", title);
                    searchResult.put("link", link);
                    searchResult.put("snippet", snippet);
                    results.add(searchResult);
                }
            }
            
        } catch (IOException e) {
            System.err.println("搜尋失敗: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 獲取網頁內容
     */
    public String fetchWebContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();
            
            // 移除腳本和樣式
            doc.select("script, style").remove();
            
            // 獲取主要內容
            String content = doc.body().text();
            
            // 限制內容長度
            if (content.length() > 3000) {
                content = content.substring(0, 3000) + "...";
            }
            
            return content;
            
        } catch (IOException e) {
            System.err.println("獲取網頁內容失敗: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 搜尋並整理結果為文字格式
     */
    public String searchAndFormat(String query, int maxResults) {
        List<Map<String, String>> results = search(query, maxResults);
        
        if (results.isEmpty()) {
            return "❌ 未找到相關搜尋結果。可能是網路問題或查詢過於具體。";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("## 🔍 網頁搜尋結果：「").append(query).append("」\n\n");
        formatted.append("找到 **").append(results.size()).append("** 筆相關資料：\n\n");
        formatted.append("---\n\n");
        
        int index = 1;
        for (Map<String, String> result : results) {
            formatted.append("### ").append(index++).append(". ")
                    .append(result.get("title"))
                    .append("\n\n");
            formatted.append("📝 **摘要：**  \n")
                    .append(result.get("snippet")).append("\n\n");
            formatted.append("🔗 **來源：** [")
                    .append(result.get("link"))
                    .append("](").append(result.get("link")).append(")\n\n");
            formatted.append("---\n\n");
        }
        
        return formatted.toString();
    }
}
