package sim;

// Sub-experiment A: FTE 수량 결정 에이전트
public class EmployerAgentFTE extends Agent {
    private static final int REPS = 5;
    private static final int MAX_TOTAL_WORKERS = 10; // 비현실적 응답 상한

    private Decision wave1Decision; // Wave 2 프롬프트 주입용

    public EmployerAgentFTE(String id) {
        super(id);
    }

    public void setWave1Decision(Decision d) {
        this.wave1Decision = d;
    }

    public Decision getWave1Decision() {
        return wave1Decision;
    }

    @Override
    public Decision decide(Scenario scenario) {
        int totalFT = 0, totalPT = 0, totalST = 0;
        int validReps = 0;

        for (int rep = 0; rep < REPS; rep++) {
            String prompt = buildPrompt(scenario);
            String response = LLMClient.call(prompt, 0.5);
            Decision d = ResponseParser.parseFTE(response);
            if (d != null && isValid(d)) {
                totalFT += d.fullTime;
                totalPT += d.partTime;
                totalST += d.shortTime;
                validReps++;
            } else {
                System.err.println("[WARN] Agent " + id + " rep " + rep + " 파싱 실패: " + response);
            }
        }

        if (validReps == 0) {
            System.err.println("[ERROR] Agent " + id + " 유효 응답 0 — 기본값 사용");
            return new Decision(0, 2, 1); // 마감 시간 없을 때 기본값
        }

        // 5 reps 평균을 반올림해서 정수로
        return new Decision(
            (int) Math.round((double) totalFT / validReps),
            (int) Math.round((double) totalPT / validReps),
            (int) Math.round((double) totalST / validReps)
        );
    }

    private String buildPrompt(Scenario scenario) {
        String base =
            "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
            "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n" +
            "현재 최저임금은 10,320원이며, 이것이 시작 시급 기준입니다.\n\n" +
            "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
            "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n\n";

        if (scenario.type == Scenario.Type.WAVE1) {
            return base +
                "앞으로 다음과 같이 직원을 고용할 계획입니다 (점주 본인 제외):\n" +
                "- 전일제 (주 40시간 이상): ___명\n" +
                "- 파트타임 (주 15~40시간): ___명\n" +
                "- 초단시간 (주 15시간 미만): ___명\n\n" +
                "숫자만 답해 주세요. 예: 0 2 1";
        }

        // Wave 2: 이전 고용현황 주입
        String history = String.format(
            "현재 귀하의 고용 현황은 다음과 같습니다 (점주 본인 제외):\n" +
            "- 전일제 (주 40시간 이상): %d명\n" +
            "- 파트타임 (주 15~40시간): %d명\n" +
            "- 초단시간 (주 15시간 미만): %d명\n\n",
            wave1Decision.fullTime, wave1Decision.partTime, wave1Decision.shortTime
        );

        if (scenario.type == Scenario.Type.WAVE2_TREATMENT) {
            return base + history +
                "내년부터 최저임금이 12,000원으로 인상됩니다.\n" +
                "이를 고려하여 앞으로의 고용 계획을 결정해 주세요 (점주 본인 제외):\n" +
                "- 전일제 (주 40시간 이상): ___명\n" +
                "- 파트타임 (주 15~40시간): ___명\n" +
                "- 초단시간 (주 15시간 미만): ___명\n\n" +
                "반드시 결정해 주세요. 숫자만 답해 주세요. 예: 0 2 1";
        } else { // WAVE2_CONTROL
            return base + history +
                "최저임금은 현재 수준(10,320원)을 유지합니다.\n" +
                "앞으로의 고용 계획을 결정해 주세요 (점주 본인 제외):\n" +
                "- 전일제 (주 40시간 이상): ___명\n" +
                "- 파트타임 (주 15~40시간): ___명\n" +
                "- 초단시간 (주 15시간 미만): ___명\n\n" +
                "반드시 결정해 주세요. 숫자만 답해 주세요. 예: 0 2 1";
        }
    }

    @Override
    public String buildReasonPrompt(Scenario scenario, Decision w1, Decision w2) {
        return String.format(
            "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
            "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n\n" +
            "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
            "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n\n" +
            "최저임금 인상 전(10,320원) 귀하의 고용 현황:\n" +
            "- 전일제: %d명, 파트타임: %d명, 초단시간: %d명\n\n" +
            "최저임금이 12,000원으로 인상된 후 귀하의 결정:\n" +
            "- 전일제: %d명, 파트타임: %d명, 초단시간: %d명\n\n" +
            "인상 전후 고용 계획이 위와 같이 결정된 가장 중요한 이유를 한 문장으로 설명해 주세요.",
            w1.fullTime, w1.partTime, w1.shortTime,
            w2.fullTime, w2.partTime, w2.shortTime
        );
    }

    private boolean isValid(Decision d) {
        if (d.fullTime < 0 || d.partTime < 0 || d.shortTime < 0) return false;
        if (d.totalWorkers() > MAX_TOTAL_WORKERS) return false;
        return true;
    }
}
