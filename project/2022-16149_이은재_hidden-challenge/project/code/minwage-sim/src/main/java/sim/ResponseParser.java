package sim;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser {

    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern ROBOT_YES = Pattern.compile("예|yes|도입|true", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROBOT_NO = Pattern.compile("아니오|no|미도입|false", Pattern.CASE_INSENSITIVE);

    // Sub-exp A: 숫자 3개 파싱
    public static Decision parseFTE(String response) {
        if (response == null || response.isBlank()) return null;
        List<Integer> nums = extractNumbers(response);
        if (nums.size() < 3) return null;
        return new Decision(nums.get(0), nums.get(1), nums.get(2));
    }

    // Sub-exp B: 숫자 3개 + 예/아니오 파싱
    public static Decision parseRobot(String response) {
        if (response == null || response.isBlank()) return null;
        List<Integer> nums = extractNumbers(response);
        if (nums.size() < 3) return null;

        Boolean robot = parseRobotDecision(response);
        if (robot == null) return null;

        return new Decision(nums.get(0), nums.get(1), nums.get(2), robot);
    }

    private static List<Integer> extractNumbers(String text) {
        List<Integer> result = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            result.add(Integer.parseInt(m.group()));
        }
        return result;
    }

    private static Boolean parseRobotDecision(String text) {
        // 먼저 "아니오"를 찾아야 함 (부분 매칭에서 "예"가 먼저 걸릴 수 있으므로)
        if (ROBOT_NO.matcher(text).find()) return false;
        if (ROBOT_YES.matcher(text).find()) return true;
        return null;
    }

    // 사후 이유: 그대로 반환 (파싱 없음)
    public static String parseReason(String response) {
        return response == null ? "" : response.trim();
    }
}
