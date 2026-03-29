package problem1;

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
        check("n=25, m=49",
            "5 times 5 = 25\n6 times 6 = 36\n7 times 7 = 49",
            capture(() -> SquareTable.printSquareTable(25, 49)));

        check("n=1, m=23",
            "1 times 1 = 1\n2 times 2 = 4\n3 times 3 = 9\n4 times 4 = 16",
            capture(() -> SquareTable.printSquareTable(1, 23)));

        check("n=3, m=3", "",
            capture(() -> SquareTable.printSquareTable(3, 3)));

        System.out.println("\n=== Problem 1: " + passed + " passed, " + failed + " failed ===");
    }
}
