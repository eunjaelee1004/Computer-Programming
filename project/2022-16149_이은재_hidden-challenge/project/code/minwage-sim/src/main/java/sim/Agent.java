package sim;

public abstract class Agent {
    protected final String id;
    protected String condition; // "treatment" or "control" (Wave 2 배정 후 설정)

    public Agent(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public abstract Decision decide(Scenario scenario);
    public abstract String buildReasonPrompt(Scenario scenario, Decision w1Decision, Decision w2Decision);
}
