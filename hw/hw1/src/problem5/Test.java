package problem5;

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
            System.out.println("  Expected: \"" + expected + "\"");
            System.out.println("  Actual:   \"" + actual + "\"");
        }
    }

    public static void main(String[] args) {
        check("321", "3",
            capture(() -> IPAddress.countValidAddress("321")));

        check("6543", "5",
            capture(() -> IPAddress.countValidAddress("6543")));

        check("050", "0",
            capture(() -> IPAddress.countValidAddress("050")));

        System.out.println("\n=== Problem 5: " + passed + " passed, " + failed + " failed ===");
    }
}
