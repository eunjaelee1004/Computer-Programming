package sim;

import java.io.*;
import java.util.*;

// Sub-exp B 추가 사후 이유 조사
// checkpoint_b.csv에서 실제 결정을 불러와 이유만 추가로 질문
// 11,820원 (도입률 최고 20%) 및 12,320원 (급락 6%)
public class PostHocExtra {
    private static final String CKPT = "record/checkpoint_b.csv";
    private static final int SAMPLE = 20;

    public static void main(String[] args) {
        if (System.getenv("OPENAI_API_KEY") == null) {
            System.err.println("[ERROR] OPENAI_API_KEY 환경변수가 설정되지 않았습니다.");
            System.exit(1);
        }

        int[] wages = {11820, 12320};

        for (int wage : wages) {
            List<AgentDecision> rows = loadFromCheckpoint(wage);
            if (rows.isEmpty()) {
                System.out.printf("[WARN] %d원 데이터 없음%n", wage);
                continue;
            }

            // SAMPLE 수만큼만 (없으면 전체)
            List<AgentDecision> sample = rows.subList(0, Math.min(SAMPLE, rows.size()));

            Scenario sc = Scenario.sweep(wage);
            int robotYes = 0;

            System.out.printf("%n=== 사후 이유 조사: 최저임금 %,d원 ===%n", wage);

            for (AgentDecision ad : sample) {
                if (Boolean.TRUE.equals(ad.decision.robot)) robotYes++;
                EmployerAgentRobot agent = new EmployerAgentRobot(ad.id);
                String prompt = agent.buildReasonPrompt(sc, null, ad.decision);
                String reason = LLMClient.call(prompt, 0.7);
                System.out.printf("  %s [%s] %s%n",
                    ad.id,
                    Boolean.TRUE.equals(ad.decision.robot) ? "예" : "아니오",
                    ResponseParser.parseReason(reason));
            }

            System.out.printf("→ 이번 샘플 로봇 도입: %d/%d (%.1f%%)%n",
                robotYes, sample.size(), (double) robotYes / sample.size() * 100);
        }
    }

    private static List<AgentDecision> loadFromCheckpoint(int targetWage) {
        List<AgentDecision> list = new ArrayList<>();
        File f = new File(CKPT);
        if (!f.exists()) {
            // 현재 디렉토리에서도 찾아보기
            f = new File("checkpoint_b.csv");
            if (!f.exists()) f = new File("record/checkpoint_b.csv");
        }
        if (!f.exists()) {
            System.err.println("[ERROR] checkpoint_b.csv 파일을 찾을 수 없습니다.");
            return list;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 6) continue;
                int wage = Integer.parseInt(p[0].trim());
                if (wage != targetWage) continue;
                String id = p[1].trim();
                int ft = Integer.parseInt(p[2].trim());
                int pt = Integer.parseInt(p[3].trim());
                int st = Integer.parseInt(p[4].trim());
                boolean robot = Boolean.parseBoolean(p[5].trim());
                list.add(new AgentDecision(id, new Decision(ft, pt, st, robot)));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 체크포인트 로드 실패: " + e.getMessage());
        }
        System.out.printf("[체크포인트] %,d원: %d개 로드%n", targetWage, list.size());
        return list;
    }

    private record AgentDecision(String id, Decision decision) {}
}
