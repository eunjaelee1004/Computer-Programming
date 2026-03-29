package problem10;

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
        check("n=6, wait=2, forget=4", "5",
            capture(() -> SecretTelling.numSecretKnowing(6, 2, 4)));

        System.out.println("\n=== Problem 10: " + passed + " passed, " + failed + " failed ===");
    }
}
