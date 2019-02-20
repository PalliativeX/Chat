package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class ConsoleHelper {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {
        String line;
        try {
            line = reader.readLine();
            return line;
        }
        catch (IOException e) {
            System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            return readString();
        }
    }

    public static int readInt() {
        try {
            int num = Integer.parseInt(readString());
            return num;
        }
        catch (NumberFormatException e) {
            System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
            return readInt();
        }
    }

}

