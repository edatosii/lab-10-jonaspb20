package co.umariana.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArbolGenealogico {
    private static final class Nodo {
        Persona persona;
        Nodo padre;
        Nodo madre;
        Nodo progenitor;
        final List<Nodo> hijos = new ArrayList<>();
        Nodo(Persona p) { persona = p; }
    }
    private final Nodo raiz;
    private int cantidad;

    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) throw new IllegalArgumentException("El sujeto no puede ser nulo.");
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) throw new ArbolInvalidoException("El sujeto debe tener la posición 1.");
        raiz = new Nodo(sujeto); cantidad = 1;
    }
    public Persona sujeto() { return raiz.persona; }
    public int tamano() { return cantidad; }

    private Nodo buscarNodo(String p) {
        if (!Posicion.esValida(p)) return null;
        if ("1".equals(p)) return raiz;
        if (Posicion.esAscendiente(p)) {
            String bits = Integer.toBinaryString(Integer.parseInt(p));
            Nodo actual = raiz;
            for (int i = 1; i < bits.length() && actual != null; i++)
                actual = bits.charAt(i) == '0' ? actual.padre : actual.madre;
            return actual;
        }
        String[] partes = p.substring(2).split("\\.");
        Nodo actual = raiz;
        for (String parte : partes) {
            int orden = Integer.parseInt(parte);
            Nodo siguiente = null;
            for (Nodo h : actual.hijos) if (Posicion.orden(h.persona.getPosicion()) == orden) { siguiente = h; break; }
            actual = siguiente;
            if (actual == null) return null;
        }
        return actual;
    }
    public Persona buscar(String p) { Nodo n = buscarNodo(p); return n == null ? null : n.persona; }
    public boolean contiene(String p) { return buscarNodo(p) != null; }

    public Persona agregar(Persona persona) {
        if (persona == null) throw new IllegalArgumentException("La persona no puede ser nula.");
        persona.validar();
        String p = persona.getPosicion();
        if ("1".equals(p) || contiene(p)) throw new ArbolInvalidoException("La posición " + p + " ya existe.");
        if (Posicion.esAscendiente(p)) {
            String hijoPos = Posicion.posicionDelHijoEnLinea(p);
            Nodo hijo = buscarNodo(hijoPos);
            if (hijo == null) throw new ArbolInvalidoException("No existe el hijo " + hijoPos + " de " + p + ".");
            if (persona.getNacimiento() >= hijo.persona.getNacimiento())
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su hijo.");
            Nodo nodo = new Nodo(persona);
            if (Integer.parseInt(p) % 2 == 0) hijo.padre = nodo; else hijo.madre = nodo;
        } else {
            String progenitorPos = Posicion.posicionDelProgenitor(p);
            Nodo progenitor = buscarNodo(progenitorPos);
            if (progenitor == null) throw new ArbolInvalidoException("No existe el progenitor " + progenitorPos + " de " + p + ".");
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento())
                throw new ArbolInvalidoException("El descendiente debe haber nacido después de su progenitor.");
            Nodo nodo = new Nodo(persona); nodo.progenitor = progenitor; progenitor.hijos.add(nodo);
            progenitor.hijos.sort(Comparator.comparingInt(h -> Posicion.orden(h.persona.getPosicion())));
        }
        cantidad++; return persona;
    }
    public Persona agregarPadre(String p, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo n = buscarNodo(p);
        if (n == null || !(Posicion.esSujeto(p) || Posicion.esAscendiente(p)))
            throw new ArbolInvalidoException("No existe una persona ascendiente en la posición " + p + ".");
        return agregar(new Persona(Posicion.posicionDelPadre(p), nombre, nacimiento, fallecimiento));
    }
    public Persona agregarMadre(String p, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo n = buscarNodo(p);
        if (n == null || !(Posicion.esSujeto(p) || Posicion.esAscendiente(p)))
            throw new ArbolInvalidoException("No existe una persona ascendiente en la posición " + p + ".");
        return agregar(new Persona(Posicion.posicionDeLaMadre(p), nombre, nacimiento, fallecimiento));
    }
    public Persona agregarHijo(String p, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo n = buscarNodo(p);
        if (n == null || !(Posicion.esSujeto(p) || Posicion.esDescendiente(p)))
            throw new ArbolInvalidoException("No existe un progenitor en la posición " + p + ".");
        int max = 0;
        for (Nodo h : n.hijos) max = Math.max(max, Posicion.orden(h.persona.getPosicion()));
        if (max >= Posicion.MAX_ORDEN) throw new ArbolInvalidoException("No hay más posiciones de hijos disponibles.");
        return agregar(new Persona(p + "." + (max + 1), nombre, nacimiento, fallecimiento));
    }

    private int contarAncestros(Nodo n) {
        if (n == null) return 0;
        return 1 + contarAncestros(n.padre) + contarAncestros(n.madre);
    }
    private int contarDescendientes(Nodo n) {
        int total = 1;
        for (Nodo h : n.hijos) total += contarDescendientes(h);
        return total;
    }
    public int eliminar(String p) {
        if ("1".equals(p)) throw new ArbolInvalidoException("No se puede eliminar al sujeto.");
        Nodo n = buscarNodo(p);
        if (n == null) throw new ArbolInvalidoException("No existe la posición " + p + ".");
        int eliminados;
        if (Posicion.esAscendiente(p)) {
            eliminados = contarAncestros(n);
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(p));
            if (Integer.parseInt(p) % 2 == 0) hijo.padre = null; else hijo.madre = null;
        } else {
            eliminados = contarDescendientes(n);
            n.progenitor.hijos.remove(n);
        }
        cantidad -= eliminados; return eliminados;
    }
    private void recolectarAncestros(Nodo n, List<Persona> salida) {
        if (n == null) return;
        salida.add(n.persona); recolectarAncestros(n.padre, salida); recolectarAncestros(n.madre, salida);
    }
    private void recolectarDesc(Nodo n, List<Persona> salida) {
        for (Nodo h : n.hijos) { salida.add(h.persona); recolectarDesc(h, salida); }
    }
    public List<Persona> ascendientes() {
        List<Persona> r = new ArrayList<>(); recolectarAncestros(raiz.padre, r); recolectarAncestros(raiz.madre, r);
        r.sort(Comparator.comparingInt(p -> Integer.parseInt(p.getPosicion()))); return r;
    }
    public List<Persona> descendientes() { List<Persona> r = new ArrayList<>(); recolectarDesc(raiz, r); return r; }
    public List<Persona> miembros() {
        List<Persona> r = new ArrayList<>(); r.add(raiz.persona); r.addAll(ascendientes()); r.addAll(descendientes()); return r;
    }
}
