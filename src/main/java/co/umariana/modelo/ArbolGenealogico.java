package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 *     ascendientes:  árbol BINARIO. Cada persona tiene como mucho un padre (2n) y una madre (2n + 1).
 *     descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1, 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas vacías.
 *
 * REGLAS DEL ÁRBOL
 *   - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 *   - Una posición no puede repetirse.
 *   - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe haber NACIDO ANTES
 *     (año de nacimiento menor) que ese hijo.
 *   - Un descendiente solo se puede agregar si ya existe su progenitor, y debe haber NACIDO DESPUÉS
 *     (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    /** Nodo del árbol: la persona, su padre, su madre y su lista de hijos (ordenada por número de orden). */
    private static final class Nodo {
        final Persona persona;
        Nodo padre;
        Nodo madre;
        final List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int cantidad;

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser nulo.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1, no en la " + sujeto.getPosicion() + ".");
        }
        this.raiz = new Nodo(sujeto);
        this.cantidad = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        return cantidad;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    /**
     * Agrega una persona en la posición que ella misma trae (la usa el constructor del árbol a partir del texto).
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula.");
        }
        persona.validar();
        String posicion = persona.getPosicion();

        if (Posicion.esSujeto(posicion) || contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " está repetida.");
        }

        if (Posicion.esAscendiente(posicion)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + ": falta la persona " + posHijo + " (su hijo/a).");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion + " (nacida en " + persona.getNacimiento()
                        + ") debe haber nacido antes que su hijo/a " + posHijo + " (nacido en "
                        + hijo.persona.getNacimiento() + ").");
            }
            Nodo nuevo = new Nodo(persona);
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            String posProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo progenitor = buscarNodo(posProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + ": falta la persona " + posProgenitor + " (su progenitor).");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion + " (nacida en " + persona.getNacimiento()
                        + ") debe haber nacido después que su progenitor " + posProgenitor + " (nacido en "
                        + progenitor.persona.getNacimiento() + ").");
            }
            insertarHijoOrdenado(progenitor, new Nodo(persona));
        }
        cantidad++;
        return persona;
    }

    /**
     * Agrega el PADRE de la persona de esa posición, en la posición 2n (ver Posicion.posicionDelPadre).
     * Solo se pueden agregar padres al sujeto y a sus ascendientes.
     *
     * @return la persona creada
     */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        validarPuedeTenerAscendientes(posicion);
        String posPadre = Posicion.posicionDelPadre(posicion);
        return agregarAscendiente(posPadre, nombre, nacimiento, fallecimiento);
    }

    /**
     * Igual que agregarPadre, pero agrega la MADRE (posición 2n + 1).
     */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        validarPuedeTenerAscendientes(posicion);
        String posMadre = Posicion.posicionDeLaMadre(posicion);
        return agregarAscendiente(posMadre, nombre, nacimiento, fallecimiento);
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Solo se pueden agregar hijos al sujeto y a sus descendientes.
     * Su posición es la del progenitor + "." + el número siguiente al MAYOR número de hijo que ya exista
     * (o 1 si todavía no tiene hijos). Ej.: si "1" tiene los hijos 1.1 y 1.3, el nuevo es 1.4.
     *
     * @return la persona creada
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo progenitor = buscarNodo(posicion);
        if (progenitor == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (Posicion.esAscendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes, no a la posición "
                    + posicion + ".");
        }
        int siguiente = 1;
        if (!progenitor.hijos.isEmpty()) {
            Nodo ultimo = progenitor.hijos.get(progenitor.hijos.size() - 1);
            siguiente = Posicion.orden(ultimo.persona.getPosicion()) + 1;
        }
        if (siguiente > Posicion.MAX_ORDEN) {
            throw new ArbolInvalidoException("La posición " + posicion + " ya no admite más hijos (máximo " + Posicion.MAX_ORDEN + ").");
        }
        String posHijo = posicion + "." + siguiente;
        if (!Posicion.esDAboville(posHijo)) {
            throw new ArbolInvalidoException("La posición " + posHijo + " es demasiado larga (máximo "
                    + Posicion.MAX_LONGITUD + " caracteres).");
        }
        return agregar(new Persona(posHijo, nombre, nacimiento, fallecimiento));
    }

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella:
     *   - si es un ascendiente: ella y todos sus propios ascendientes (sus padres, abuelos...);
     *   - si es un descendiente: ella y todos sus propios descendientes (hijos, nietos...).
     * Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" (el sujeto no se puede eliminar) o si no existe
     */
    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }

        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
        }

        int eliminados = contarNodos(nodo);
        cantidad -= eliminados;
        return eliminados;
    }

    /**
     * @return todos los miembros en este orden: el sujeto; luego los ascendientes por número Ahnentafel
     *         ascendente; luego los descendientes en PREORDEN. La lista es una copia nueva.
     */
    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        List<Persona> resultado = new ArrayList<>();
        // Recorrido por niveles (generación por generación): dentro de cada nivel, padre antes que madre,
        // lo que equivale a ir en orden de número Ahnentafel.
        List<Nodo> nivel = new ArrayList<>();
        nivel.add(raiz);
        while (!nivel.isEmpty()) {
            List<Nodo> siguiente = new ArrayList<>();
            for (Nodo n : nivel) {
                if (n.padre != null) {
                    siguiente.add(n.padre);
                    resultado.add(n.padre.persona);
                }
                if (n.madre != null) {
                    siguiente.add(n.madre);
                    resultado.add(n.madre.persona);
                }
            }
            nivel = siguiente;
        }
        return resultado;
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        List<Persona> resultado = new ArrayList<>();
        preorden(raiz, resultado);
        return resultado;
    }

    // ------------------------------------------------------------------ auxiliares privados

    /** Busca el nodo de una posición siguiendo su camino en el árbol; null si no existe o no es válida. */
    private Nodo buscarNodo(String posicion) {
        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            Nodo actual = raiz;
            // Los bits de n después del primer 1 dicen el camino: 0 = padre, 1 = madre.
            for (int bit = 30 - Integer.numberOfLeadingZeros(n); bit >= 0 && actual != null; bit--) {
                actual = ((n >> bit) & 1) == 0 ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDAboville(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                actual = buscarHijo(actual, Integer.parseInt(partes[i]));
            }
            return actual;
        }
        return null;
    }

    /** Busca entre los hijos de un nodo el que tiene ese número de orden; null si no existe. */
    private Nodo buscarHijo(Nodo progenitor, int orden) {
        for (Nodo hijo : progenitor.hijos) {
            int ordenHijo = Posicion.orden(hijo.persona.getPosicion());
            if (ordenHijo == orden) {
                return hijo;
            }
            if (ordenHijo > orden) {
                return null; // la lista está ordenada: ya no puede aparecer
            }
        }
        return null;
    }

    /** Inserta el hijo en la lista del progenitor manteniéndola ordenada por número de orden. */
    private void insertarHijoOrdenado(Nodo progenitor, Nodo nuevo) {
        int orden = Posicion.orden(nuevo.persona.getPosicion());
        int indice = 0;
        while (indice < progenitor.hijos.size()
                && Posicion.orden(progenitor.hijos.get(indice).persona.getPosicion()) < orden) {
            indice++;
        }
        progenitor.hijos.add(indice, nuevo);
    }

    /** Revisa que la posición exista y sea el sujeto o un ascendiente (los únicos que pueden tener padres). */
    private void validarPuedeTenerAscendientes(String posicion) {
        if (!contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes, no a la posición "
                    + posicion + ".");
        }
    }

    private Persona agregarAscendiente(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                    + ": se superó el máximo de " + Posicion.MAX_AHNENTAFEL + " (10 generaciones de ascendientes).");
        }
        return agregar(new Persona(posicion, nombre, nacimiento, fallecimiento));
    }

    /** Cuenta el nodo y todo lo que cuelga de él (padre, madre e hijos). */
    private int contarNodos(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int total = 1 + contarNodos(nodo.padre) + contarNodos(nodo.madre);
        for (Nodo hijo : nodo.hijos) {
            total += contarNodos(hijo);
        }
        return total;
    }

    /** Agrega a los descendientes del nodo en preorden (cada hijo y después los hijos de ese hijo). */
    private void preorden(Nodo nodo, List<Persona> resultado) {
        for (Nodo hijo : nodo.hijos) {
            resultado.add(hijo.persona);
            preorden(hijo, resultado);
        }
    }
}