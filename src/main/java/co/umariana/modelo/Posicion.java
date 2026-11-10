package co.umariana.modelo;

public final class Posicion {
    public static final String SUJETO = "1";
    public static final int MAX_AHNENTAFEL = 2047;
    public static final int MAX_ORDEN = 9999;
    public static final int MAX_LONGITUD = 50;
    private Posicion() {}

    public static boolean esSujeto(String p) { return SUJETO.equals(p); }
    public static boolean esAhnentafel(String p) {
        if (p == null || !p.matches("[1-9][0-9]*")) return false;
        try { int n = Integer.parseInt(p); return n >= 1 && n <= MAX_AHNENTAFEL; }
        catch (NumberFormatException e) { return false; }
    }
    public static boolean esDAboville(String p) {
        if (p == null || p.length() > MAX_LONGITUD || !p.matches("1(?:\\.[1-9][0-9]{0,3})+")) return false;
        String[] partes = p.split("\\.");
        for (int i = 1; i < partes.length; i++) {
            try { if (Integer.parseInt(partes[i]) > MAX_ORDEN) return false; }
            catch (NumberFormatException e) { return false; }
        }
        return true;
    }
    public static boolean esValida(String p) { return esAhnentafel(p) || esDAboville(p); }
    public static boolean esAscendiente(String p) { return esAhnentafel(p) && !"1".equals(p); }
    public static boolean esDescendiente(String p) { return esDAboville(p); }
    private static int numero(String p) {
        if (!esAhnentafel(p)) throw new IllegalArgumentException("Posición Ahnentafel inválida: " + p);
        return Integer.parseInt(p);
    }
    public static String posicionDelPadre(String p) { return String.valueOf(numero(p) * 2); }
    public static String posicionDeLaMadre(String p) { return String.valueOf(numero(p) * 2 + 1); }
    public static String posicionDelHijoEnLinea(String p) {
        int n = numero(p);
        if (n == 1) throw new IllegalArgumentException("El sujeto no tiene hijo en línea");
        return String.valueOf(n / 2);
    }
    public static String posicionDelProgenitor(String p) {
        if (!esDescendiente(p)) throw new IllegalArgumentException("Posición d'Aboville inválida: " + p);
        return p.substring(0, p.lastIndexOf('.'));
    }
    public static int orden(String p) {
        if (!esDescendiente(p)) throw new IllegalArgumentException("Posición d'Aboville inválida: " + p);
        return Integer.parseInt(p.substring(p.lastIndexOf('.') + 1));
    }
    public static int generacion(String p) {
        if (esAhnentafel(p)) return 31 - Integer.numberOfLeadingZeros(Integer.parseInt(p));
        if (esDescendiente(p)) {
            int count = 0; for (int i = 0; i < p.length(); i++) if (p.charAt(i) == '.') count++;
            return count;
        }
        throw new IllegalArgumentException("Posición inválida: " + p);
    }
}
