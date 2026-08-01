package sim;

public class DiDResult {
    public final double treatBefore;  // 처치군 Wave 1 평균 FTE
    public final double treatAfter;   // 처치군 Wave 2 평균 FTE
    public final double ctrlBefore;   // 통제군 Wave 1 평균 FTE
    public final double ctrlAfter;    // 통제군 Wave 2 평균 FTE
    public final double did;          // DiD 추정치
    public final double se;           // 표준오차
    public final double tStat;        // t-통계량
    public final double pValue;       // p-값 (단측)

    public DiDResult(double treatBefore, double treatAfter,
                     double ctrlBefore, double ctrlAfter,
                     double did, double se) {
        this.treatBefore = treatBefore;
        this.treatAfter = treatAfter;
        this.ctrlBefore = ctrlBefore;
        this.ctrlAfter = ctrlAfter;
        this.did = did;
        this.se = se;
        this.tStat = se > 0 ? did / se : Double.NaN;
        this.pValue = se > 0 ? oneTailPValue(this.tStat) : Double.NaN;
    }

    // 단측 p-값 근사 (t분포, df=98 → 정규 근사)
    // H₁: DiD < 0 (최저임금 인상 → 고용 감소) → 좌측 단측
    private static double oneTailPValue(double t) {
        return standardNormalCDF(t); // P(Z < t)
    }

    private static double standardNormalCDF(double z) {
        // Abramowitz & Stegun 근사 (양수 z 전용)
        if (z < 0) return 1.0 - standardNormalCDF(-z);
        double t = 1.0 / (1.0 + 0.2316419 * z);
        double poly = t * (0.319381530
            + t * (-0.356563782
            + t * (1.781477937
            + t * (-1.821255978
            + t * 1.330274429))));
        return 1.0 - (1.0 / Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * z * z) * poly;
    }

    public void print() {
        System.out.println("=== Sub-experiment A DiD 결과 (C&K Table 3 구조) ===");
        System.out.printf("처치군 Wave1 평균 FTE: %.3f%n", treatBefore);
        System.out.printf("처치군 Wave2 평균 FTE: %.3f  (변화: %+.3f)%n", treatAfter, treatAfter - treatBefore);
        System.out.printf("통제군 Wave1 평균 FTE: %.3f%n", ctrlBefore);
        System.out.printf("통제군 Wave2 평균 FTE: %.3f  (변화: %+.3f)%n", ctrlAfter, ctrlAfter - ctrlBefore);
        System.out.printf("DiD 추정치: %.3f%n", did);
        System.out.printf("표준오차:   %.3f%n", se);
        System.out.printf("t-통계량:   %.3f%n", tStat);
        System.out.printf("p-값 (단측, H₁: DiD<0): %.4f%n", pValue);
        System.out.println(pValue < 0.05 ? "→ H₀ 기각 (p<0.05, 고용 감소 유의)" : "→ H₀ 기각 실패");
    }
}
