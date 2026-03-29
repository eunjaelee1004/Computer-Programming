import java.util.Scanner;

public class TicTacToe {
    public static void main(String[] args) {
        // Your code goes here
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        int n = 3;
        int [][] board = new int [n][n];
        for (int i = 0; i < input.length(); i++) {
            int row = i / n;
            int col = i % n;
            board[row][col] = input.charAt(i) - '0';
        }
//        printBoard(board);
        deterWinner(board);
    }

    public static void printBoard(int[][] board) {
        System.out.println("Board:");
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
    }

    public static void deterWinner(int[][] board) {
        //number of marks differ
        int count_zero = 0;
        int count_one = 0;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (board[r][c] == 0) {
                    count_zero += 1;
                } else {
                    count_one += 1;
                }
            }
        }
        if (Math.abs(count_zero - count_one) > 1) {
            System.out.println("Invalid game.");
            return;
        }
        //number of row check
        int row_zero = 0;
        int row_one = 0;
        //1.horizontal
        for (int r = 0; r < board.length; r++) {
            int hor_zero = 0;
            int hor_one = 0;
            for (int c = 0; c < board[0].length; c++) {
                if (board[r][c] == 0) {
                    hor_zero += 1;
                } else {
                    hor_one += 1;
                }
            }
            if (hor_zero == board.length) {
                row_zero += 1;
            }
            if (hor_one == board.length) {
                row_one += 1;
            }
        }
        //2.vertical
        for (int c = 0; c < board[0].length; c++) {
            int ver_zero = 0;
            int ver_one = 0;
            for (int r = 0; r < board.length; r++) {
                if (board[r][c] == 0) {
                    ver_zero += 1;
                } else {
                    ver_one += 1;
                }
            }
            if (ver_zero == board.length) {
                row_zero += 1;
            }
            if (ver_one == board.length) {
                row_one += 1;
            }
        }
        //3-1.main diagonal
        int mdiag_zero = 0;
        int mdiag_one = 0;
        for (int r = 0; r < board.length; r++) {
            if (board[r][r] == 0) {
                mdiag_zero += 1;
            } else {
                mdiag_one += 1;
            }
        }
        if (mdiag_zero == board.length) {
            row_zero += 1;
        }
        if (mdiag_one == board.length) {
            row_one += 1;
        }
        //3-2.anti diagonal
        int adiag_zero = 0;
        int adiag_one = 0;
        int n = board.length;
        for (int r = 0; r < n; r++) {
            int c = n-1-r;
            if (board[r][c] == 0) {
                adiag_zero += 1;
            } else {
                adiag_one += 1;
            }
        }
        if (adiag_zero == board.length) {
            row_zero += 1;
        }
        if (adiag_one == board.length) {
            row_one += 1;
        }
        //determine
        if (row_zero == 0 && row_one == 0) {
            System.out.println("Tie.");
            return;
        } else if (row_zero == row_one) {
            System.out.println("Invalid game.");
            return;
        } else if (row_zero > row_one) {
            System.out.println("Player 0 win.");
            return;
        } else {
            System.out.println("Player 1 win.");
            return;
        }
    }
}
