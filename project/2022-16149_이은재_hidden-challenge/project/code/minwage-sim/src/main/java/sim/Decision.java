package sim;

public class Decision {
    public final int fullTime;  // 전일제
    public final int partTime;  // 파트타임
    public final int shortTime; // 초단시간
    public final Boolean robot; // null = Sub-exp A (해당 없음)

    public Decision(int fullTime, int partTime, int shortTime, Boolean robot) {
        this.fullTime = fullTime;
        this.partTime = partTime;
        this.shortTime = shortTime;
        this.robot = robot;
    }

    // Sub-exp A용 (로봇 없음)
    public Decision(int fullTime, int partTime, int shortTime) {
        this(fullTime, partTime, shortTime, null);
    }

    public double getFTE() {
        return fullTime * 1.0 + partTime * 0.5 + shortTime * 0.25;
    }

    public int totalWorkers() {
        return fullTime + partTime + shortTime;
    }

    @Override
    public String toString() {
        String base = String.format("FT=%d PT=%d ST=%d FTE=%.2f", fullTime, partTime, shortTime, getFTE());
        return robot != null ? base + " Robot=" + (robot ? "예" : "아니오") : base;
    }
}
