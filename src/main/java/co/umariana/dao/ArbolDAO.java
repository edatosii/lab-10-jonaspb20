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

    // ETAPA 4.1: GUARDAR
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