package sim;

import java.io.*;
import java.util.*;

public class Simulation {
    private static final int N_AGENTS = 100;
    private static final int N_TREATMENT = 50;
    private static final int N_SWEEP_AGENTS = 50;
    private static final int POST_HOC_SAMPLE = 20;

    private static final String CKPT_A_W1 = "checkpoint_a_wave1.csv";
    private static final String CKPT_A_W2 = "checkpoint_a_wave2.csv";
    private static final String CKPT_B    = "checkpoint_b.csv";

    private final List<EmployerAgentFTE> fteAgents = new ArrayList<>();
    private final List<EmployerAgentRobot> robotAgents = new ArrayList<>();

    public Simulation() {
        for (int i = 1; i <= N_AGENTS; i++) {
            fteAgents.add(new EmployerAgentFTE("FTE-" + String.format("%03d", i)));
        }
        for (int i = 1; i <= N_SWEEP_AGENTS; i++) {
            robotAgents.add(new EmployerAgentRobot("ROBOT-" + String.format("%03d", i)));
        }
    }

    // ──────────────────────────────────────────────
    // Phase 1: Sub-experiment A
    // ──────────────────────────────────────────────

    public DiDResult runSubExpA() {
        System.out.println("\n=== Phase 1: Sub-experiment A 시작 ===");

        // Step 1: Wave 1
        System.out.println("[Step 1] Wave 1 — 전체 " + N_AGENTS + "개 에이전트");
        Scenario w1 = Scenario.wave1();
        Map<String, Decision> w1Ckpt = loadFTECheckpoint(CKPT_A_W1);

        for (EmployerAgentFTE agent : fteAgents) {
            Decision d;
            if (w1Ckpt.containsKey(agent.getId())) {
                d = w1Ckpt.get(agent.getId());
                System.out.printf("  %s Wave1: %s [체크포인트]%n", agent.getId(), d);
            } else {
                d = agent.decide(w1);
                appendFTECheckpoint(CKPT_A_W1, agent.getId(), d);
                System.out.printf("  %s Wave1: %s%n", agent.getId(), d);
            }
            agent.setWave1Decision(d);
        }

        // Step 2: 50:50 무작위 배정 (seed 고정 → 재현 가능)
        System.out.println("[Step 2] 무작위 50:50 배정");
        List<EmployerAgentFTE> shuffled = new ArrayList<>(fteAgents);
        Collections.shuffle(shuffled, new Random(42));
        List<EmployerAgentFTE> treatment = shuffled.subList(0, N_TREATMENT);
        List<EmployerAgentFTE> control = shuffled.subList(N_TREATMENT, N_AGENTS);
        treatment.forEach(a -> a.setCondition("treatment"));
        control.forEach(a -> a.setCondition("control"));

        // Step 3: Wave 2
        System.out.println("[Step 3] Wave 2 — 처치군 " + N_TREATMENT + "개, 통제군 " + (N_AGENTS - N_TREATMENT) + "개");
        Map<EmployerAgentFTE, Decision> w2Decisions = new LinkedHashMap<>();
        Map<String, Decision> w2Ckpt = loadFTECheckpoint(CKPT_A_W2);

        Scenario w2t = Scenario.wave2Treatment();
        for (EmployerAgentFTE agent : treatment) {
            Decision d;
            if (w2Ckpt.containsKey(agent.getId())) {
                d = w2Ckpt.get(agent.getId());
                System.out.printf("  %s [처치] Wave2: %s [체크포인트]%n", agent.getId(), d);
            } else {
                d = agent.decide(w2t);
                appendFTECheckpoint(CKPT_A_W2, agent.getId(), d);
                System.out.printf("  %s [처치] Wave2: %s%n", agent.getId(), d);
            }
            w2Decisions.put(agent, d);
        }

        Scenario w2c = Scenario.wave2Control();
        for (EmployerAgentFTE agent : control) {
            Decision d;
            if (w2Ckpt.containsKey(agent.getId())) {
                d = w2Ckpt.get(agent.getId());
                System.out.printf("  %s [통제] Wave2: %s [체크포인트]%n", agent.getId(), d);
            } else {
                d = agent.decide(w2c);
                appendFTECheckpoint(CKPT_A_W2, agent.getId(), d);
                System.out.printf("  %s [통제] Wave2: %s%n", agent.getId(), d);
            }
            w2Decisions.put(agent, d);
        }

        // Step 4: DiD 계산
        System.out.println("[Step 4] DiD 계산");
        DiDResult result = calculateDiD(treatment, control, w2Decisions);
        result.print();

        printCompositionAnalysis(treatment, control, w2Decisions);

        // Step 5: 사후 이유 질문
        System.out.println("[Step 5] 사후 이유 질문 (처치군 " + POST_HOC_SAMPLE + "개 샘플)");
        List<EmployerAgentFTE> sample = new ArrayList<>(treatment).subList(0, POST_HOC_SAMPLE);
        for (EmployerAgentFTE agent : sample) {
            Decision w1d = agent.getWave1Decision();
            Decision w2d = w2Decisions.get(agent);
            String prompt = agent.buildReasonPrompt(w2t, w1d, w2d);
            String reason = LLMClient.call(prompt, 0.5);
            System.out.printf("  %s 이유: %s%n", agent.getId(), ResponseParser.parseReason(reason));
        }

        return result;
    }

    private DiDResult calculateDiD(List<EmployerAgentFTE> treatment,
                                    List<EmployerAgentFTE> control,
                                    Map<EmployerAgentFTE, Decision> w2) {
        double treatW1 = treatment.stream().mapToDouble(a -> a.getWave1Decision().getFTE()).average().orElse(0);
        double treatW2 = treatment.stream().mapToDouble(a -> w2.get(a).getFTE()).average().orElse(0);
        double ctrlW1  = control.stream().mapToDouble(a -> a.getWave1Decision().getFTE()).average().orElse(0);
        double ctrlW2  = control.stream().mapToDouble(a -> w2.get(a).getFTE()).average().orElse(0);

        double did = (treatW2 - treatW1) - (ctrlW2 - ctrlW1);

        double varTreat = variance(treatment.stream().mapToDouble(a -> w2.get(a).getFTE() - a.getWave1Decision().getFTE()).toArray());
        double varCtrl  = variance(control.stream().mapToDouble(a -> w2.get(a).getFTE() - a.getWave1Decision().getFTE()).toArray());
        double se = Math.sqrt(varTreat / treatment.size() + varCtrl / control.size());

        return new DiDResult(treatW1, treatW2, ctrlW1, ctrlW2, did, se);
    }

    private void printCompositionAnalysis(List<EmployerAgentFTE> treatment,
                                           List<EmployerAgentFTE> control,
                                           Map<EmployerAgentFTE, Decision> w2) {
        System.out.println("\n--- 고용 구성 변화 부가 분석 (H₀_comp) ---");

        double treatST_W1 = treatment.stream()
            .mapToDouble(a -> ratio(a.getWave1Decision())).average().orElse(0);
        double treatST_W2 = treatment.stream()
            .mapToDouble(a -> ratio(w2.get(a))).average().orElse(0);
        double ctrlST_W1 = control.stream()
            .mapToDouble(a -> ratio(a.getWave1Decision())).average().orElse(0);
        double ctrlST_W2 = control.stream()
            .mapToDouble(a -> ratio(w2.get(a))).average().orElse(0);

        double compDiD = (treatST_W2 - treatST_W1) - (ctrlST_W2 - ctrlST_W1);
        System.out.printf("초단시간 비중 처치군 W1→W2: %.3f → %.3f (%+.3f)%n", treatST_W1, treatST_W2, treatST_W2 - treatST_W1);
        System.out.printf("초단시간 비중 통제군 W1→W2: %.3f → %.3f (%+.3f)%n", ctrlST_W1, ctrlST_W2, ctrlST_W2 - ctrlST_W1);
        System.out.printf("구성 DiD (초단시간 비중): %+.3f%n", compDiD);
        System.out.println(compDiD > 0 ? "→ H₀_comp 지지 (초단시간 증가)" : "→ H₀_comp 기각 (초단시간 미증가)");
    }

    private double ratio(Decision d) {
        int total = d.totalWorkers();
        return total == 0 ? 0 : (double) d.shortTime / total;
    }

    // ──────────────────────────────────────────────
    // Phase 2: Sub-experiment B
    // ──────────────────────────────────────────────

    public void runSubExpB() {
        System.out.println("\n=== Phase 2: Sub-experiment B 시작 (로봇 바리스타 sweep) ===");

        double[] wages = {10320, 10820, 11320, 11820, 12320, 12820, 13320, 13820, 14320, 14820};
        double[] robotRates = new double[wages.length];
        int thresholdIdx = -1;

        Map<String, Decision> sweepCkpt = loadSweepCheckpoint(CKPT_B);

        for (int i = 0; i < wages.length; i++) {
            Scenario sc = Scenario.sweep(wages[i]);
            int robotYesCount = 0;

            for (EmployerAgentRobot agent : robotAgents) {
                String key = (int) wages[i] + "_" + agent.getId();
                Decision d;
                if (sweepCkpt.containsKey(key)) {
                    d = sweepCkpt.get(key);
                } else {
                    d = agent.decide(sc);
                    appendSweepCheckpoint(CKPT_B, wages[i], agent.getId(), d);
                }
                if (Boolean.TRUE.equals(d.robot)) robotYesCount++;
            }

            robotRates[i] = (double) robotYesCount / N_SWEEP_AGENTS;
            System.out.printf("  최저임금 %,6.0f원: 로봇 도입 %.1f%% (%d/%d)%n",
                wages[i], robotRates[i] * 100, robotYesCount, N_SWEEP_AGENTS);

            if (thresholdIdx == -1 && robotRates[i] >= 0.5) {
                thresholdIdx = i;
            }
        }

        System.out.println("\n--- Sub-exp B 결과 ---");
        if (thresholdIdx >= 0) {
            System.out.printf("자동화 임계 최저임금: %,6.0f원 (로봇 도입 %.1f%%)%n",
                wages[thresholdIdx], robotRates[thresholdIdx] * 100);
        } else {
            System.out.println("임계점 미도달 — sweep 범위 확장 필요");
        }

        // 사후 이유 질문
        double targetWage = thresholdIdx >= 0 ? wages[thresholdIdx] : wages[wages.length - 1];
        System.out.printf("%n[Step 8] 사후 이유 질문 (최저임금 %,.0f원, %d개 샘플)%n", targetWage, POST_HOC_SAMPLE);
        Scenario sc = Scenario.sweep(targetWage);
        List<EmployerAgentRobot> sample = new ArrayList<>(robotAgents).subList(0, POST_HOC_SAMPLE);

        for (EmployerAgentRobot agent : sample) {
            Decision d = agent.decide(sc);
            String prompt = agent.buildReasonPrompt(sc, null, d);
            String reason = LLMClient.call(prompt, 0.5);
            System.out.printf("  %s [%s] 이유: %s%n",
                agent.getId(), Boolean.TRUE.equals(d.robot) ? "예" : "아니오",
                ResponseParser.parseReason(reason));
        }
    }

    // ──────────────────────────────────────────────
    // 체크포인트 유틸
    // ──────────────────────────────────────────────

    private Map<String, Decision> loadFTECheckpoint(String filename) {
        Map<String, Decision> map = new LinkedHashMap<>();
        File f = new File(filename);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) continue;
                String id = parts[0].trim();
                int ft = Integer.parseInt(parts[1].trim());
                int pt = Integer.parseInt(parts[2].trim());
                int st = Integer.parseInt(parts[3].trim());
                map.put(id, new Decision(ft, pt, st));
            }
        } catch (IOException e) {
            System.err.println("[WARN] 체크포인트 로드 실패 (" + filename + "): " + e.getMessage());
        }
        if (!map.isEmpty())
            System.out.println("[체크포인트] " + filename + " 에서 " + map.size() + "개 로드");
        return map;
    }

    private void appendFTECheckpoint(String filename, String id, Decision d) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
            pw.printf("%s,%d,%d,%d%n", id, d.fullTime, d.partTime, d.shortTime);
        } catch (IOException e) {
            System.err.println("[WARN] 체크포인트 저장 실패 (" + filename + "): " + e.getMessage());
        }
    }

    private Map<String, Decision> loadSweepCheckpoint(String filename) {
        Map<String, Decision> map = new LinkedHashMap<>();
        File f = new File(filename);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;
                String key = parts[0].trim() + "_" + parts[1].trim();
                int ft = Integer.parseInt(parts[2].trim());
                int pt = Integer.parseInt(parts[3].trim());
                int st = Integer.parseInt(parts[4].trim());
                boolean robot = Boolean.parseBoolean(parts[5].trim());
                map.put(key, new Decision(ft, pt, st, robot));
            }
        } catch (IOException e) {
            System.err.println("[WARN] 체크포인트 로드 실패 (" + filename + "): " + e.getMessage());
        }
        if (!map.isEmpty())
            System.out.println("[체크포인트] " + filename + " 에서 " + map.size() + "개 로드");
        return map;
    }

    private void appendSweepCheckpoint(String filename, double wage, String id, Decision d) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
            pw.printf("%d,%s,%d,%d,%d,%b%n",
                (int) wage, id, d.fullTime, d.partTime, d.shortTime, Boolean.TRUE.equals(d.robot));
        } catch (IOException e) {
            System.err.println("[WARN] 체크포인트 저장 실패 (" + filename + "): " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────

    private static double variance(double[] values) {
        if (values.length == 0) return 0;
        double mean = Arrays.stream(values).average().orElse(0);
        return Arrays.stream(values).map(v -> (v - mean) * (v - mean)).sum() / (values.length - 1);
    }
}
