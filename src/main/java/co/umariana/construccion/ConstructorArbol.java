package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * Las líneas pueden venir en cualquier orden; los errores asociados a una línea empiezan con "Línea N: ".
 */
public final class ConstructorArbol {

    /** Una persona leída del texto, junto con el número de línea de donde salió. */
    private record Entrada(Persona persona, int linea) {
    }

    private ConstructorArbol() {
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos la línea del sujeto (posición 1).");
        }

        // 1. Leer cada línea (las en blanco se ignoran, pero se cuentan para el número de línea).
        String[] lineas = texto.split("\r?\n", -1);
        List<Entrada> entradas = new ArrayList<>();
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            if (linea.isBlank()) {
                continue;
            }
            int numero = i + 1;
            Persona persona = leerLinea(linea, numero);
            for (Entrada anterior : entradas) {
                if (anterior.persona().getPosicion().equals(persona.getPosicion())) {
                    throw errorEnLinea(numero, "la posición " + persona.getPosicion() + " está repetida.");
                }
            }
            entradas.add(new Entrada(persona, numero));
        }

        // 2. Ordenar para que cada persona quede después de la que necesita:
        //    sujeto, ascendientes por número Ahnentafel y descendientes por generación.
        entradas.sort(Comparator
                .comparingInt(ConstructorArbol::grupo)
                .thenComparingInt(ConstructorArbol::clave)
                .thenComparingInt(Entrada::linea));

        if (entradas.isEmpty() || !Posicion.esSujeto(entradas.get(0).persona().getPosicion())) {
            throw new ArbolInvalidoException("Falta el sujeto: debe haber una línea con la posición 1.");
        }

        // 3. Armar el árbol; las reglas las valida el propio árbol.
        Entrada sujeto = entradas.get(0);
        ArbolGenealogico arbol;
        try {
            arbol = new ArbolGenealogico(sujeto.persona());
        } catch (IllegalArgumentException e) {
            throw errorEnLinea(sujeto.linea(), e.getMessage());
        }
        for (Entrada entrada : entradas.subList(1, entradas.size())) {
            try {
                arbol.agregar(entrada.persona());
            } catch (IllegalArgumentException e) {
                throw errorEnLinea(entrada.linea(), e.getMessage());
            }
        }
        return arbol;
    }

    /** Convierte una línea de texto en una Persona validada; si algo falla informa el número de línea. */
    private static Persona leerLinea(String linea, int numero) {
        String[] campos = linea.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw errorEnLinea(numero, "debe tener 3 o 4 campos separados por ';' (posicion;nombre;nacimiento;fallecimiento)"
                    + " y tiene " + campos.length + ".");
        }
        String posicion = campos[0].trim();
        String nombre = campos[1].trim();

        int nacimiento;
        try {
            nacimiento = Integer.parseInt(campos[2].trim());
        } catch (NumberFormatException e) {
            throw errorEnLinea(numero, "el año de nacimiento debe ser un número entero.");
        }

        Integer fallecimiento = null;
        if (campos.length == 4 && !campos[3].isBlank()) {
            try {
                fallecimiento = Integer.valueOf(campos[3].trim());
            } catch (NumberFormatException e) {
                throw errorEnLinea(numero, "el año de fallecimiento debe ser un número entero (o quedar vacío).");
            }
        }

        Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
        try {
            persona.validar();
        } catch (IllegalArgumentException e) {
            throw errorEnLinea(numero, e.getMessage());
        }
        return persona;
    }

    /** 0 = sujeto, 1 = ascendientes, 2 = descendientes. */
    private static int grupo(Entrada entrada) {
        String posicion = entrada.persona().getPosicion();
        if (Posicion.esSujeto(posicion)) {
            return 0;
        }
        return Posicion.esAscendiente(posicion) ? 1 : 2;
    }

    /** Ascendientes: su número Ahnentafel. Descendientes: su generación (para que el progenitor vaya primero). */
    private static int clave(Entrada entrada) {
        String posicion = entrada.persona().getPosicion();
        if (Posicion.esAscendiente(posicion)) {
            return Integer.parseInt(posicion);
        }
        return Posicion.generacion(posicion);
    }

    /** Crea el error con el prefijo "Línea N: " y la primera letra del mensaje en minúscula. */
    private static ArbolInvalidoException errorEnLinea(int numero, String mensaje) {
        String texto = mensaje == null ? "dato inválido." : mensaje;
        if (!texto.isEmpty()) {
            texto = Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
        }
        return new ArbolInvalidoException("Línea " + numero + ": " + texto);
    }
}