package problem8;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Test {
    private static int passed = 0, failed = 0;

    private static String capture(Runnable r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        r.run();
        System.setOut(old);
        return baos.toString().replaceAll("\\r\\n", "\n").replaceAll("\\n$", "");
    }

    private static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("PASS [" + name + "]");
        } else {
            failed++;
            System.out.println("FAIL [" + name + "]");
            System.out.println("  Expected: \"" + expected.replace("\n", "\\n") + "\"");
            System.out.println("  Actual:   \"" + actual.replace("\n", "\\n") + "\"");
        }
    }

    public static void main(String[] args) {
        check("3x2 * 2x3",
            "3 1 2\n7 3 6\n11 5 10",
            capture(() -> MatrixMultiplication.multiply(
                new int[][]{{1, 2}, {3, 4}, {5, 6}},
                new int[][]{{1, 1, 2}, {1, 0, 0}})));

        check("2x3 * 2x2 invalid", "invalid",
            capture(() -> MatrixMultiplication.multiply(
                new int[][]{{1, 2, 3}, {4, 5, 6}},
                new int[][]{{1, 2}, {3, 4}})));

        System.out.println("\n=== Problem 8: " + passed + " passed, " + failed + " failed ===");
    }
}
