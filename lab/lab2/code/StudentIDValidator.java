import java.util.Scanner; // For string input

public class StudentIDValidator {
    public static void main(String[] args) {
        // Your code goes here
        Scanner scanner = new Scanner(System.in);
        String studentID = scanner.next();

        if (studentID.length() != 10) {
            System.out.println("The input length should be 10.");
            return;
        }

        if (studentID.charAt(4) != '-') {
            System.out.println("Fifth character should be '-'.");
            return;
        }

        for (int i = 0; i < studentID.length(); i++) {
            if (i == 4) {
                continue;
            }
            char ch = studentID.charAt(i);
            if (ch < '0' || ch > '9') {
                System.out.println("Contains an invalid digit.");
                return;
            }
        }

        System.out.println(studentID + " is valid.");
    }
}