package eu.appguru;

/**
 *
 * @author lars
 */
public class Unit {
    
    class SyntaxError extends RuntimeException {
        private SyntaxError(String message) {
            super(message);
        }
    }
    
    public double value;
    public String name;

    public Unit(double value, String name) {
        this.value = value;
        this.name = name;
    }
    
    public static String cm(double value) {
        return new Unit(value, "cm").toString();
    }
    
    public Unit(String literal) {
        int i=literal.length()-1;
        boolean literal_exists = false;
        for (; i > -1; i--) {
            char n = literal.charAt(i);
            if (n == '.' || (n >= '0' && n <= '9')) {
                literal_exists = true;
                break;
            }
        }
        if (!literal_exists) {
            throw new SyntaxError("Invalid unit, missing literal");
        }
        value = Double.parseDouble(literal.substring(0, i+1));
        name = literal.substring(i+1);
    }
    
    @Override
    public String toString() {
        return Double.toString(value)+name;
    }
}
