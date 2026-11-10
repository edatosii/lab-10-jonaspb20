package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConstructorArbol {
    private ConstructorArbol() {}
    private record Entrada(Persona persona, int linea) {}

    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) throw new ArbolInvalidoException("El texto está vacío.");
        String[] lineas = texto.split("\\r?\\n", -1);
        List<Entrada> entradas = new ArrayList<>();
        Set<String> posiciones = new HashSet<>();
        boolean haySujeto = false;
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty()) continue;
            String[] campos = lineas[i].split(";", -1);
            if (campos.length < 3 || campos.length > 4)
                throw error(i + 1, "cada línea debe tener 3 o 4 campos separados por punto y coma.");
            String posicion = campos[0].trim();
            String nombre = campos[1].trim();
            String nacimientoTexto = campos[2].trim();
            String fallecimientoTexto = campos.length == 4 ? campos[3].trim() : "";
            int nacimiento;
            Integer fallecimiento = null;
            try { nacimiento = Integer.parseInt(nacimientoTexto); }
            catch (NumberFormatException e) { throw error(i + 1, "el año de nacimiento debe ser un número entero."); }
            if (!fallecimientoTexto.isEmpty()) {
                try { fallecimiento = Integer.valueOf(fallecimientoTexto); }
                catch (NumberFormatException e) { throw error(i + 1, "el año de fallecimiento debe ser un número entero."); }
            }
            Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
            try { persona.validar(); }
            catch (IllegalArgumentException e) { throw error(i + 1, e.getMessage()); }
            if (!posiciones.add(posicion)) throw error(i + 1, "la posición " + posicion + " está repetida.");
            if (Posicion.esSujeto(posicion)) haySujeto = true;
            entradas.add(new Entrada(persona, i + 1));
        }
        if (entradas.isEmpty()) throw new ArbolInvalidoException("El texto está vacío.");
        if (!haySujeto) throw new ArbolInvalidoException("Falta la persona de la posición 1 (sujeto).");

        entradas.sort((a, b) -> {
            String pa = a.persona().getPosicion(), pb = b.persona().getPosicion();
            boolean aa = Posicion.esAhnentafel(pa), ab = Posicion.esAhnentafel(pb);
            if (aa != ab) return aa ? -1 : 1;
            if (aa) return Integer.compare(Integer.parseInt(pa), Integer.parseInt(pb));
            int g = Integer.compare(Posicion.generacion(pa), Posicion.generacion(pb));
            if (g != 0) return g;
            return compararDaboville(pa, pb);
        });
        ArbolGenealogico arbol = null;
        for (Entrada e : entradas) {
            try {
                if (Posicion.esSujeto(e.persona().getPosicion())) arbol = new ArbolGenealogico(e.persona());
                else arbol.agregar(e.persona());
            } catch (IllegalArgumentException ex) {
                throw error(e.linea(), ex.getMessage());
            }
        }
        return arbol;
    }
    private static int compararDaboville(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 1; i < Math.min(x.length, y.length); i++) {
            int c = Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]));
            if (c != 0) return c;
        }
        return Integer.compare(x.length, y.length);
    }
    private static ArbolInvalidoException error(int linea, String mensaje) {
        return new ArbolInvalidoException("Línea " + linea + ": " + mensaje);
    }
}
