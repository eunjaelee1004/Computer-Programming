package sim;

/**
 * 파이프라인 검증용 미니 실험
 * - Sub-exp A: 에이전트 3개, reps 2회 (Wave 1 → Wave 2 처치)
 * - Sub-exp B: 에이전트 3개, reps 2회, 임금 수준 3개만 sweep
 * - 각 단계에서 raw 응답도 출력해서 파싱 정상 여부 확인
 *
 * 실행: java -cp out sim.TestRun
 */
public class TestRun {

    private static final int TEST_AGENTS = 10;
    private static final int TEST_REPS = 2;
    private static final int TEST_REASON_SAMPLE = 5; // 사후 이유 질문 에이전트 수
    private static final double TEST_TEMPERATURE = 0.5;

    public static void main(String[] args) {
        if (System.getenv("OPENAI_API_KEY") == null) {
            System.err.println("[ERROR] OPENAI_API_KEY 환경변수 미설정");
            System.exit(1);
        }

        System.out.println("========================================");
        System.out.println("  파이프라인 테스트 실행 (미니 스케일)");
        System.out.println("  에이전트: " + TEST_AGENTS + "개 / reps: " + TEST_REPS + "회");
        System.out.println("========================================\n");

        testSubExpA();
        testSubExpB();

        System.out.println("\n========================================");
        System.out.println("  테스트 완료");
        System.out.println("========================================");
    }

    // ──────────────────────────────────────────────
    // Sub-exp A 테스트
    // ──────────────────────────────────────────────
    private static void testSubExpA() {
        System.out.println("=== [Sub-exp A 테스트] ===\n");

        Scenario w1 = Scenario.wave1();
        Decision[] w1Decisions = new Decision[TEST_AGENTS];

        // Step 1: Wave 1
        System.out.println("--- Wave 1 ---");
        for (int i = 0; i < TEST_AGENTS; i++) {
            System.out.printf("[에이전트 A-%d] Wave 1 프롬프트 전송 중...%n", i + 1);

            int totalFT = 0, totalPT = 0, totalST = 0, valid = 0;
            for (int rep = 0; rep < TEST_REPS; rep++) {
                String prompt = buildFTEWave1Prompt(w1);
                String raw = LLMClient.call(prompt, TEST_TEMPERATURE);
                System.out.printf("  rep%d raw 응답: [%s]%n", rep + 1, raw);

                Decision d = ResponseParser.parseFTE(raw);
                if (d != null) {
                    System.out.printf("  rep%d 파싱 결과: %s%n", rep + 1, d);
                    totalFT += d.fullTime; totalPT += d.partTime; totalST += d.shortTime;
                    valid++;
                } else {
                    System.out.printf("  rep%d 파싱 실패!%n", rep + 1);
                }
            }

            if (valid > 0) {
                w1Decisions[i] = new Decision(
                    (int) Math.round((double) totalFT / valid),
                    (int) Math.round((double) totalPT / valid),
                    (int) Math.round((double) totalST / valid)
                );
                System.out.printf("→ [에이전트 A-%d] Wave 1 평균: %s%n%n", i + 1, w1Decisions[i]);
            } else {
                w1Decisions[i] = new Decision(0, 2, 1); // 파싱 실패 시 기본값
                System.out.printf("→ [에이전트 A-%d] 전체 실패 — 기본값 사용: %s%n%n", i + 1, w1Decisions[i]);
            }
        }

        // Step 2: Wave 2 처치군
        System.out.println("--- Wave 2 (처치군: 최저임금 12,000원) ---");
        Scenario w2t = Scenario.wave2Treatment();
        for (int i = 0; i < TEST_AGENTS; i++) {
            System.out.printf("[에이전트 A-%d] Wave 2 프롬프트 전송 중...%n", i + 1);

            int totalFT = 0, totalPT = 0, totalST = 0, valid = 0;
            for (int rep = 0; rep < TEST_REPS; rep++) {
                String prompt = buildFTEWave2Prompt(w2t, w1Decisions[i]);
                String raw = LLMClient.call(prompt, TEST_TEMPERATURE);
                System.out.printf("  rep%d raw 응답: [%s]%n", rep + 1, raw);

                Decision d = ResponseParser.parseFTE(raw);
                if (d != null) {
                    System.out.printf("  rep%d 파싱 결과: %s%n", rep + 1, d);
                    totalFT += d.fullTime; totalPT += d.partTime; totalST += d.shortTime;
                    valid++;
                } else {
                    System.out.printf("  rep%d 파싱 실패!%n", rep + 1);
                }
            }

            if (valid > 0) {
                Decision w2d = new Decision(
                    (int) Math.round((double) totalFT / valid),
                    (int) Math.round((double) totalPT / valid),
                    (int) Math.round((double) totalST / valid)
                );
                System.out.printf("→ [에이전트 A-%d] Wave 2 평균: %s%n", i + 1, w2d);
                System.out.printf("   FTE 변화: %.2f → %.2f (%+.2f)%n%n",
                    w1Decisions[i].getFTE(), w2d.getFTE(), w2d.getFTE() - w1Decisions[i].getFTE());
            }
        }

        // Step 3: 사후 이유 (TEST_REASON_SAMPLE개 에이전트)
        System.out.printf("--- 사후 이유 질문 (%d개 에이전트) ---%n", TEST_REASON_SAMPLE);
        for (int i = 0; i < Math.min(TEST_REASON_SAMPLE, TEST_AGENTS); i++) {
            if (w1Decisions[i] == null) continue;
            Decision testW2 = new Decision(0, 1, 2); // 임의 Wave 2 결정 (테스트용)
            String reasonPrompt = String.format(
                "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
                "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n\n" +
                "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
                "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n\n" +
                "최저임금 인상 전(10,320원) 귀하의 고용 현황:\n" +
                "- 전일제: %d명, 파트타임: %d명, 초단시간: %d명\n\n" +
                "최저임금이 12,000원으로 인상된 후 귀하의 결정:\n" +
                "- 전일제: %d명, 파트타임: %d명, 초단시간: %d명\n\n" +
                "인상 전후 고용 계획이 위와 같이 결정된 가장 중요한 이유를 한 문장으로 설명해 주세요.",
                w1Decisions[i].fullTime, w1Decisions[i].partTime, w1Decisions[i].shortTime,
                testW2.fullTime, testW2.partTime, testW2.shortTime
            );
            String raw = LLMClient.call(reasonPrompt, TEST_TEMPERATURE);
            System.out.printf("[에이전트 A-%d] %s%n", i + 1, ResponseParser.parseReason(raw));
        }
    }

    // ──────────────────────────────────────────────
    // Sub-exp B 테스트
    // ──────────────────────────────────────────────
    private static void testSubExpB() {
        System.out.println("\n=== [Sub-exp B 테스트] ===\n");

        double[] testWages = {10320, 12000, 14820}; // 3개 수준만

        Decision[] lastDecisions = new Decision[TEST_AGENTS]; // 사후 이유용 마지막 결정 저장
        double lastWage = testWages[testWages.length - 1];

        for (double wage : testWages) {
            System.out.printf("--- 최저임금 %,.0f원 ----%n", wage);
            Scenario sc = Scenario.sweep(wage);
            int robotYes = 0;

            for (int i = 0; i < TEST_AGENTS; i++) {
                System.out.printf("[에이전트 B-%d]%n", i + 1);

                int agentRobotYes = 0, valid = 0;
                int totalFT = 0, totalPT = 0, totalST = 0;
                for (int rep = 0; rep < TEST_REPS; rep++) {
                    String prompt = buildRobotPrompt(sc);
                    String raw = LLMClient.call(prompt, TEST_TEMPERATURE);
                    System.out.printf("  rep%d raw 응답: [%s]%n", rep + 1, raw);

                    Decision d = ResponseParser.parseRobot(raw);
                    if (d != null) {
                        System.out.printf("  rep%d 파싱 결과: %s%n", rep + 1, d);
                        totalFT += d.fullTime; totalPT += d.partTime; totalST += d.shortTime;
                        if (Boolean.TRUE.equals(d.robot)) agentRobotYes++;
                        valid++;
                    } else {
                        System.out.printf("  rep%d 파싱 실패!%n", rep + 1);
                    }
                }

                if (valid > 0) {
                    boolean robotDecision = agentRobotYes > valid / 2.0;
                    if (robotDecision) robotYes++;
                    Decision avg = new Decision(
                        (int) Math.round((double) totalFT / valid),
                        (int) Math.round((double) totalPT / valid),
                        (int) Math.round((double) totalST / valid),
                        robotDecision
                    );
                    System.out.printf("→ [에이전트 B-%d] 평균: %s%n%n", i + 1, avg);
                    if (wage == lastWage) lastDecisions[i] = avg;
                }
            }

            System.out.printf("최저임금 %,.0f원 → 로봇 도입 %d/%d (%.0f%%)%n%n",
                wage, robotYes, TEST_AGENTS, (double) robotYes / TEST_AGENTS * 100);
        }

        // 사후 이유 질문 (마지막 임금 수준, TEST_REASON_SAMPLE개 에이전트)
        System.out.printf("--- 사후 이유 질문 (최저임금 %,.0f원, %d개 에이전트) ----%n", lastWage, TEST_REASON_SAMPLE);
        Scenario reasonSc = Scenario.sweep(lastWage);
        int ptCost = EmployerAgentRobot.calcPTMonthlyCostMan((int) lastWage);
        for (int i = 0; i < Math.min(TEST_REASON_SAMPLE, TEST_AGENTS); i++) {
            if (lastDecisions[i] == null) continue;
            String reasonPrompt = String.format(
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
                (int) lastWage, ptCost,
                lastDecisions[i].fullTime, lastDecisions[i].partTime, lastDecisions[i].shortTime,
                Boolean.TRUE.equals(lastDecisions[i].robot) ? "예" : "아니오",
                (int) reasonSc.robotMonthlyRent
            );
            String raw = LLMClient.call(reasonPrompt, TEST_TEMPERATURE);
            System.out.printf("[에이전트 B-%d] [%s] %s%n",
                i + 1,
                Boolean.TRUE.equals(lastDecisions[i].robot) ? "예" : "아니오",
                ResponseParser.parseReason(raw));
        }
    }

    // ──────────────────────────────────────────────
    // 프롬프트 빌더 (테스트용 인라인)
    // ──────────────────────────────────────────────
    private static String buildFTEWave1Prompt(Scenario sc) {
        return "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
            "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n" +
            "현재 최저임금은 10,320원이며, 이것이 시작 시급 기준입니다.\n\n" +
            "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
            "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n\n" +
            "앞으로 다음과 같이 직원을 고용할 계획입니다 (점주 본인 제외):\n" +
            "- 전일제 (주 40시간 이상): ___명\n" +
            "- 파트타임 (주 15~40시간): ___명\n" +
            "- 초단시간 (주 15시간 미만): ___명\n\n" +
            "숫자만 답해 주세요. 예: 0 2 1";
    }

    private static String buildFTEWave2Prompt(Scenario sc, Decision w1) {
        return "당신은 소형 저가 커피 프랜차이즈 점포를 운영하는 점주입니다.\n" +
            "점포 규모: 약 20석, 하루 방문객 150~200명 수준.\n" +
            "현재 최저임금은 10,320원이며, 이것이 시작 시급 기준입니다.\n\n" +
            "※ 주 15시간 이상 근무자(파트타임)는 주휴수당이 발생하여 실질 인건비는 시급의 약 1.2배입니다.\n" +
            "※ 주 15시간 미만 근무자(초단시간)는 주휴수당이 없습니다.\n\n" +
            String.format("현재 귀하의 고용 현황은 다음과 같습니다 (점주 본인 제외):\n" +
            "- 전일제 (주 40시간 이상): %d명\n" +
            "- 파트타임 (주 15~40시간): %d명\n" +
            "- 초단시간 (주 15시간 미만): %d명\n\n", w1.fullTime, w1.partTime, w1.shortTime) +
            "내년부터 최저임금이 12,000원으로 인상됩니다.\n" +
            "이를 고려하여 앞으로의 고용 계획을 결정해 주세요 (점주 본인 제외):\n" +
            "- 전일제 (주 40시간 이상): ___명\n" +
            "- 파트타임 (주 15~40시간): ___명\n" +
            "- 초단시간 (주 15시간 미만): ___명\n\n" +
            "반드시 결정해 주세요. 숫자만 답해 주세요. 예: 0 2 1";
    }

    private static String buildRobotPrompt(Scenario sc) {
        int wage = (int) sc.currentMinWage;
        int rent = (int) sc.robotMonthlyRent;
        int ptMonthlyCost = EmployerAgentRobot.calcPTMonthlyCostMan(wage);
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
}
