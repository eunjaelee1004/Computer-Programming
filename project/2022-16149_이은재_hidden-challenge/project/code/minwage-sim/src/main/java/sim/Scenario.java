package sim;

public class Scenario {
    public enum Type { WAVE1, WAVE2_TREATMENT, WAVE2_CONTROL, SWEEP }

    public final Type type;
    public final double currentMinWage;   // 현행 최저임금 (10320)
    public final double treatmentMinWage; // 처치 최저임금 (12000 or sweep 값)
    public final double robotMonthlyRent; // 로봇 렌탈비 (만원, 150)

    private Scenario(Type type, double current, double treatment, double robot) {
        this.type = type;
        this.currentMinWage = current;
        this.treatmentMinWage = treatment;
        this.robotMonthlyRent = robot;
    }

    public static Scenario wave1() {
        return new Scenario(Type.WAVE1, 10320, 10320, 150);
    }

    public static Scenario wave2Treatment() {
        return new Scenario(Type.WAVE2_TREATMENT, 10320, 12000, 150);
    }

    public static Scenario wave2Control() {
        return new Scenario(Type.WAVE2_CONTROL, 10320, 10320, 150);
    }

    public static Scenario sweep(double minWage) {
        return new Scenario(Type.SWEEP, minWage, minWage, 150);
    }
}
