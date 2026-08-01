package sim;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LLMClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");

    // Rate limit 대응: 요청 간 최소 간격 (ms)
    private static final long REQUEST_DELAY_MS = 200;
    private static final int MAX_RETRIES = 3;

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public static String call(String prompt, double temperature) {
        String body = buildRequestBody(prompt, temperature);

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Thread.sleep(REQUEST_DELAY_MS);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429) {
                    long wait = parseRetryAfter(response) * 1000L;
                    System.err.println("[WARN] Rate limit — " + wait + "ms 대기 후 재시도");
                    Thread.sleep(wait > 0 ? wait : 5000);
                    continue;
                }

                if (response.statusCode() != 200) {
                    System.err.println("[ERROR] HTTP " + response.statusCode() + ": " + response.body());
                    return null;
                }

                String content = parseContent(response.body());
                if (content == null) {
                    System.err.println("[ERROR] content 파싱 실패. 응답 본문: " + response.body());
                }
                return content;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (IOException e) {
                System.err.println("[WARN] 네트워크 오류 (시도 " + (attempt + 1) + "): " + e.getMessage());
                if (attempt == MAX_RETRIES - 1) return null;
                try { Thread.sleep(2000L * (attempt + 1)); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); return null;
                }
            }
        }
        return null;
    }

    private static String buildRequestBody(String prompt, double temperature) {
        // 수동 JSON 구성 (외부 라이브러리 없이)
        String escapedPrompt = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
        return String.format(
            "{\"model\":\"%s\",\"temperature\":%.1f,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
            MODEL, temperature, escapedPrompt
        );
    }

    private static String parseContent(String json) {
        // "content": "..." 패턴 추출 (공백 유무 모두 대응)
        String key = "\"content\":";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        // 공백·콜론 이후 첫 '"' 찾기
        while (start < json.length() && json.charAt(start) != '"') start++;
        if (start >= json.length()) return null;
        start++; // '"' 건너뜀

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') sb.append('\n');
                else if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private static long parseRetryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After")
            .map(v -> { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 10L; } })
            .orElse(10L);
    }
}
