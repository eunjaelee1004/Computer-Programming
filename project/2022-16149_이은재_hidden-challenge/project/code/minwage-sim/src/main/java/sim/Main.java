package sim;

public class Main {
    public static void main(String[] args) {
        if (System.getenv("OPENAI_API_KEY") == null) {
            System.err.println("[ERROR] OPENAI_API_KEY 환경변수가 설정되지 않았습니다.");
            System.err.println("  export OPENAI_API_KEY=sk-...");
            System.exit(1);
        }

        String mode = args.length > 0 ? args[0] : "all";

        Simulation sim = new Simulation();

        switch (mode) {
            case "a":
                sim.runSubExpA();
                break;
            case "b":
                sim.runSubExpB();
                break;
            case "all":
            default:
                sim.runSubExpA();
                sim.runSubExpB();
                break;
        }
    }
}
