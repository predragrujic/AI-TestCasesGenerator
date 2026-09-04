package testcasegeneration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TestCasesGenerator {

  private static final String DEFAULT_URL = "https://www.spacejam.com/1996/";

  static void main(String[] args) throws Exception {
    String url = (args.length >= 1) ? args[0] : DEFAULT_URL;
    System.out.println("Testing site: " + url);

    String apiKey = System.getenv("GEMINI_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("Missing GEMINI_API_KEY environment variable.");
      return;
    }

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest siteRequest = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();
    HttpResponse<String> siteResponse = client.send(siteRequest, HttpResponse.BodyHandlers.ofString());
    String html = siteResponse.body();
    if (html.length() > 8000) {
      html = html.substring(0, 8000); // limit the size
    }

    String promptText = "Based on the following HTML content from the site " + url +
        ", create a list of concrete test cases (functional, UI, edge cases) " +
        "that a QA engineer would test. Use this exact format for each test case:\n\n" +
        "TC001 - [short test case name]\n" +
        "Steps: 1) ... 2) ... 3) ...\n" +
        "Expected result: ...\n\n" +
        "Continue in order with TC002, TC003, etc. for each next test case. " +
        "Steps should describe concrete user actions (e.g. opened page X, entered data Y, clicked button Z). " +
        "IMPORTANT: if there are multiple similar navigation links (e.g. multiple icons that all lead to different internal pages in the same way), " +
        "do not create a separate test case for each one and do not list each link individually - combine them into ONE short test case " +
        "that generally describes testing navigation for all icons/links in the main menu, without a detailed list of every path.\n\n" +
        "At the very end, after all test cases, add a 'BUG REPORT' section listing all problems, " +
        "inconsistencies, potential bugs, or issues you noticed in the HTML code " +
        "(e.g. broken links, missing alt attributes, mixed HTTP/HTTPS content, outdated code, accessibility). " +
        "Format for each bug:\n\n" +
        "BUG01 - [short issue name]\n" +
        "Description: ...\n" +
        "Expected behavior: ...\n" +
        "Severity: (Low/Medium/High)\n\n" +
        "Continue in order with BUG02, BUG03, etc. If there are no obvious bugs, list at least minor issues or improvement suggestions. " +
        "NOTE: the entire response should be plain text without markdown formatting (no #, ##, **, tables with | characters), " +
        "since it is printed directly to the console.\n\nHTML:\n" + html;

    String jsonBody = "{"
        + "\"contents\":[{\"parts\":[{\"text\":" + jsonEscape(promptText) + "}]}]"
        + "}";

    String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + apiKey;

    HttpRequest apiRequest = HttpRequest.newBuilder()
        .uri(URI.create(endpoint))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
        .build();

    HttpResponse<String> apiResponse = client.send(apiRequest, HttpResponse.BodyHandlers.ofString());

    String extractedText = extractFirstTextField(apiResponse.body());
    if (extractedText != null) {
      System.out.println("=== Test cases for " + url + " ===\n");
      System.out.println(extractedText);
    } else {
      System.out.println("No text found in the response, here is the raw response:");
      System.out.println(apiResponse.body());
    }
  }

  private static String extractFirstTextField(String json) {
    String marker = "\"text\": \"";
    int start = json.indexOf(marker);
    if (start == -1) {
      marker = "\"text\":\"";
      start = json.indexOf(marker);
      if (start == -1) return null;
    }
    int i = start + marker.length();
    StringBuilder sb = new StringBuilder();
    while (i < json.length()) {
      char c = json.charAt(i);
      if (c == '\\' && i + 1 < json.length()) {
        char next = json.charAt(i + 1);
        switch (next) {
          case 'n': sb.append('\n'); i += 2; continue;
          case 'r': sb.append('\r'); i += 2; continue;
          case 't': sb.append('\t'); i += 2; continue;
          case '"': sb.append('"'); i += 2; continue;
          case '\\': sb.append('\\'); i += 2; continue;
          case 'u':
            if (i + 5 < json.length()) {
              String hex = json.substring(i + 2, i + 6);
              sb.append((char) Integer.parseInt(hex, 16));
              i += 6;
              continue;
            }
            break;
          default:
            sb.append(next);
            i += 2;
            continue;
        }
      }
      if (c == '"') {
        break; // end of string
      }
      sb.append(c);
      i++;
    }
    return sb.toString();
  }

  private static String jsonEscape(String text) {
    StringBuilder sb = new StringBuilder();
    sb.append('"');
    for (char c : text.toCharArray()) {
      switch (c) {
        case '"': sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\t': sb.append("\\t"); break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
    return sb.toString();
  }
}

