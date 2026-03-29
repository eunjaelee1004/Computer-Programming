package problem2;

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
        check("n=5",
            "0 1 1 2 3\neven sum = 2",
            capture(() -> FibonacciNumbers.printFibonacciNumbers(5)));

        check("n=8",
            "0 1 1 2 3 5 8 13\neven sum = 10",
            capture(() -> FibonacciNumbers.printFibonacciNumbers(8)));

        check("n=30",
            "0 1 1 2 3 5 8 13 21 34 55 89 144 233 377 " +
            "610 987 1597 2584 4181 6765 10946 17711 28657 46368 75025 121393 " +
            "196418 317811 514229\n" +
            "even sum = 57114",
            capture(() -> FibonacciNumbers.printFibonacciNumbers(30)));

        System.out.println("\n=== Problem 2: " + passed + " passed, " + failed + " failed ===");
    }
}
