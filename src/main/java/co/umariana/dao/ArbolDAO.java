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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object): la única clase que habla con la base de datos. Guarda los árboles genealógicos
 * en las tablas "arboles" y "miembros" (ver db/schema.sql).
 */
public class ArbolDAO {

    // ETAPA 4.1. Guardar un árbol nuevo
    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.isBlank() || nombre.length() > 100 || arbol == null) {
            return -1;
        }

        Connection con = null;

        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            long id;

            String sqlArbol = "INSERT INTO arboles (nombre) VALUES (?)";

            try (PreparedStatement ps = con.prepareStatement(sqlArbol, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("No se pudo obtener el ID del árbol.");
                    }
                    id = rs.getLong(1);
                }
            }

            insertarMiembros(con, id, arbol);

            con.commit();
            return id;

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
            System.out.println(e.getMessage());
            return -1;

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
                try {
                    con.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    // Inserta todos los miembros en el orden entregado por el árbol.
    private void insertarMiembros(Connection con, long arbolId, ArbolGenealogico arbol) throws SQLException {
        String sql = "INSERT INTO miembros "
                + "(arbol_id, posicion, nombre, nacimiento, fallecimiento) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Persona persona : arbol.miembros()) {
                ps.setLong(1, arbolId);
                ps.setString(2, persona.getPosicion());
                ps.setString(3, persona.getNombre());
                ps.setInt(4, persona.getNacimiento());

                if (persona.getFallecimiento() == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, persona.getFallecimiento());
                }

                ps.executeUpdate();
            }
        }
    }

    // ETAPA 4.2. Listar
    public List<ResumenArbol> listar() {
        List<ResumenArbol> resumenes = new ArrayList<>();

        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total "
                + "FROM arboles a "
                + "LEFT JOIN miembros m ON m.arbol_id = a.id "
                + "GROUP BY a.id, a.nombre, a.creado "
                + "ORDER BY a.id";

        try (Connection con = ConexionBD.conectar();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("creado");
                LocalDateTime creado = timestamp == null ? null : timestamp.toLocalDateTime();

                resumenes.add(new ResumenArbol(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getInt("total"),
                        creado
                ));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }

        return resumenes;
    }

    // ETAPA 4.3. Cargar un árbol
    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento "
                + "FROM miembros WHERE arbol_id = ? ORDER BY id";

        try (Connection con = ConexionBD.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                ArbolGenealogico arbol = null;

                while (rs.next()) {
                    int fallecimientoLeido = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : fallecimientoLeido;

                    Persona persona = new Persona(
                            rs.getString("posicion"),
                            rs.getString("nombre"),
                            rs.getInt("nacimiento"),
                            fallecimiento
                    );

                    if (arbol == null) {
                        arbol = new ArbolGenealogico(persona);
                    } else {
                        arbol.agregar(persona);
                    }
                }

                return arbol;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    // ETAPA 4.4. Actualizar
    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }

        Connection con = null;

        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            String consulta = "SELECT id FROM arboles WHERE id = ?";

            try (PreparedStatement ps = con.prepareStatement(consulta)) {
                ps.setLong(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            String borrar = "DELETE FROM miembros WHERE arbol_id = ?";

            try (PreparedStatement ps = con.prepareStatement(borrar)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            insertarMiembros(con, id, arbol);

            con.commit();
            return true;

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
            System.out.println(e.getMessage());
            return false;

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
                try {
                    con.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    // ETAPA 4.5. Eliminar
    public boolean eliminar(long id) {
        String sql = "DELETE FROM arboles WHERE id = ?";

        try (Connection con = ConexionBD.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
