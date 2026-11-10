package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ArbolDAO {
    private static final String INSERT_MIEMBRO =
            "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.isBlank() || nombre.length() > 100 || arbol == null) return -1;
        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);
            long id;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO arboles (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó el id del árbol.");
                    id = keys.getLong(1);
                }
            }
            insertarMiembros(con, id, arbol);
            con.commit();
            return id;
        } catch (SQLException | RuntimeException e) {
            System.out.println(e.getMessage());
            if (con != null) try { con.rollback(); } catch (SQLException ex) { System.out.println(ex.getMessage()); }
            return -1;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); } catch (SQLException e) { System.out.println(e.getMessage()); }
                try { con.close(); } catch (SQLException e) { System.out.println(e.getMessage()); }
            }
        }
    }

    private void insertarMiembros(Connection con, long id, ArbolGenealogico arbol) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_MIEMBRO)) {
            for (Persona p : arbol.miembros()) {
                ps.setLong(1, id);
                ps.setString(2, p.getPosicion());
                ps.setString(3, p.getNombre());
                ps.setInt(4, p.getNacimiento());
                if (p.getFallecimiento() == null) ps.setNull(5, Types.INTEGER);
                else ps.setInt(5, p.getFallecimiento());
                ps.executeUpdate();
            }
        }
    }

    public List<ResumenArbol> listar() {
        List<ResumenArbol> resultado = new ArrayList<>();
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total " +
                "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id " +
                "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new ResumenArbol(rs.getLong("id"), rs.getString("nombre"),
                        rs.getInt("total"), rs.getTimestamp("creado").toLocalDateTime()));
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
        return resultado;
    }

    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros WHERE arbol_id = ? ORDER BY id";
        try (Connection con = ConexionBD.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                ArbolGenealogico arbol = null;
                while (rs.next()) {
                    int f = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : f;
                    Persona persona = new Persona(rs.getString("posicion"), rs.getString("nombre"),
                            rs.getInt("nacimiento"), fallecimiento);
                    if (arbol == null) arbol = new ArbolGenealogico(persona);
                    else arbol.agregar(persona);
                }
                return arbol;
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) return false;
        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);
            try (PreparedStatement comprobar = con.prepareStatement("SELECT id FROM arboles WHERE id = ?")) {
                comprobar.setLong(1, id);
                try (ResultSet rs = comprobar.executeQuery()) {
                    if (!rs.next()) { con.rollback(); return false; }
                }
            }
            try (PreparedStatement borrar = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                borrar.setLong(1, id);
                borrar.executeUpdate();
            }
            insertarMiembros(con, id, arbol);
            con.commit();
            return true;
        } catch (SQLException | RuntimeException e) {
            System.out.println(e.getMessage());
            if (con != null) try { con.rollback(); } catch (SQLException ex) { System.out.println(ex.getMessage()); }
            return false;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); } catch (SQLException e) { System.out.println(e.getMessage()); }
                try { con.close(); } catch (SQLException e) { System.out.println(e.getMessage()); }
            }
        }
    }

    public boolean eliminar(long id) {
        String sql = "DELETE FROM arboles WHERE id = ?";
        try (Connection con = ConexionBD.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
