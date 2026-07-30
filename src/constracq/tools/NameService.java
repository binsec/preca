package constracq.tools;

public class NameService {
    
    public static int getVarNumber(String column) {
        int result = 0;
        for (int i = 0; i < column.length(); i++) {
            result *= 26;
            result += column.charAt(i) - 'A' + 1;
        }
        return result-1;
    }

    public static String getVarName(int num) {
        final StringBuilder sb = new StringBuilder();

        while (num >=  0) {
            int numChar = (num % 26)  + 65;
            sb.append((char)numChar);
            num = (num  / 26) - 1;
        }
        return sb.reverse().toString();
    }
}
