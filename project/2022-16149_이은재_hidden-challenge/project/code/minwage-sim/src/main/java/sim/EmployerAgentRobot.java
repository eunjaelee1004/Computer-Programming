package sim;

// Sub-experiment B: 고용 구성 + 로봇 바리스타 자동화 sweep 에이전트
public class EmployerAgentRobot extends Agent {
    private static final int REPS = 5;
    private static final int MAX_TOTAL_WORKERS = 10;

    public EmployerAgentRobot(String id) {
        super(id);
    }

    @Override
    public Decision decide(Scenario scenario) {
        int totalFT = 0, totalPT = 0, totalST = 0, robotYes = 0;
        int validReps = 0;

        for (int rep = 0; rep < REPS; rep++) {
            String prompt = buildPrompt(scenario);
            String response = LLMClient.call(prompt, 0.5);
            Decision d = ResponseParser.parseRobot(response);
            if (d != null && isValid(d)) {
                totalFT += d.fullTime;
                totalPT += d.partTime;
                totalST += d.shortTime;
                if (Boolean.TRUE.equals(d.robot)) robotYes++;
                validReps++;
            } else {
                System.err.println("[WARN] Agent " + id + " rep " + rep + " 파싱 실패: " + response);
            }
        }

        if (validReps == 0) {
            System.err.println("[ERROR] Agent " + id + " 유효 응답 0 — 기본값 사용");
            return new Decision(0, 2, 1, false);
        }

        // 고용 수는 평균, 로봇은 과반수 투표
        return new Decision(
            (int) Math.round((double) totalFT / validReps),
            (int) Math.round((double) totalPT / validReps),
            (int) Math.round((double) totalST / validReps),
            robotYes > validReps / 2.0
        );
    }

    private String buildPrompt(Scenario scenario) {
        int wage = (int) scenario.currentMinWage;
        int rent = (int) scenario.robotMonthlyRent;
        int ptMonthlyCost = calcPTMonthlyCostMan(wage); // 만원 단위
        return String.format(
            "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
            "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n" +
            "현재 최저임금은 %,d원입니다.\n\n" +
            "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
            "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n\n" +
            "점포 운영을 위한 인력 구성을 결정해 주세요 (점주 본인 제외):\n" +
            "- 전일제 (주 40시간 이상): ___명\n" +
            "- 파트타임 (주 15~40시간): ___명\n" +
            "- 초단시간 (주 15시간 미만): ___명\n" +
            "- 로봇 바리스타 도입: 예 / 아니오\n" +
            "  (음료 제조 역할에서 직원 1명 분량 담당, 월 렌탈비 %d만원)\n" +
            "  ※ 참고: 현재 최저임금 기준 파트타임 1명(주 25시간) 월 인건비 ≈ %d만원 (주휴수당 포함)\n\n" +
            "반드시 결정해 주세요. 숫자와 예/아니오로만 답해 주세요. 예: 0 2 1 아니오",
            wage, rent, ptMonthlyCost
        );
    }

    // 파트타임 1명(주 25시간) 월 인건비 계산 (만원, 반올림)
    // = 시급 × 25h × (52/12)주 × 1.2(주휴수당)
    static int calcPTMonthlyCostMan(int wage) {
        double monthly = wage * 25.0 * (52.0 / 12.0) * 1.2;
        return (int) Math.round(monthly / 10000);
    }

    @Override
    public String buildReasonPrompt(Scenario scenario, Decision w1, Decision w2) {
        int wage = (int) scenario.currentMinWage;
        int rent = (int) scenario.robotMonthlyRent;
        int ptMonthlyCost = calcPTMonthlyCostMan(wage);
        return String.format(
            "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
            "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n" +
            "현재 최저임금은 %,d원입니다.\n\n" +
            "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
            "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n" +
            "※ 참고: 현재 최저임금 기준 파트타임 1명(주 25시간) 월 인건비 ≈ %d만원 (주휴수당 포함)\n\n" +
            "귀하는 다음과 같이 결정했습니다:\n" +
            "- 전일제: %d명, 파트타임: %d명, 초단시간: %d명\n" +
            "- 로봇 바리스타 도입: %s (월 렌탈비 %d만원)\n\n" +
            "인력 구성(전일제/파트타임/초단시간)과 로봇 바리스타 도입 여부를 위와 같이 결정한 가장 중요한 이유를 한 문장으로 설명해 주세요.",
            wage, ptMonthlyCost,
            w2.fullTime, w2.partTime, w2.shortTime,
            Boolean.TRUE.equals(w2.robot) ? "예" : "아니오",
            rent
        );
    }

    private boolean isValid(Decision d) {
        if (d.fullTime < 0 || d.partTime < 0 || d.shortTime < 0) return false;
        if (d.totalWorkers() > MAX_TOTAL_WORKERS) return false;
        if (d.robot == null) return false;
        return true;
    }
}
