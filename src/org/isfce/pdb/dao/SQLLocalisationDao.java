package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.isfce.pdb.model.Localisation;
import org.isfce.pdb.model.Piece;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLLocalisationDao implements ILocalisationDao {

    private static String SQL_GET_FROM_ID = """
            select l.FKELEMENT_LOC, l.FKPIECE_LOC, l.X_LOC, l.Y_LOC, l.A_LOC
            from TLOCALISATION l
            where l.FKELEMENT_LOC = ?
            """;

    private static String SQL_INSERT = """
            insert into TLOCALISATION (FKELEMENT_LOC, FKPIECE_LOC, X_LOC, Y_LOC, A_LOC)
            values (?, ?, ?, ?, ?)
            """;

    private static String SQL_DELETE = """
            delete from TLOCALISATION
            where FKELEMENT_LOC = ?
            """;

    private static String SQL_UPDATE = """
            update TLOCALISATION
            set FKPIECE_LOC = ?,
                X_LOC = ?,
                Y_LOC = ?,
                A_LOC = ?
            where FKELEMENT_LOC = ?
            """;

    private DAOFactory factory;
    private Connection connexion;

    public SQLLocalisationDao(DAOFactory factory) {
        this.factory = factory;
        this.connexion = factory.getConnection();
    }

    @Override
    public Optional<Localisation> getFromID(Integer id) {
        Localisation obj = null;

        try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_FROM_ID)) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Piece piece = factory.getPieceDAO()
                        .getFromID(rs.getInt("FKPIECE_LOC"))
                        .get();

                obj = Localisation.builder()
                        .elementId(rs.getInt("FKELEMENT_LOC"))
                        .piece(piece)
                        .x(rs.getDouble("X_LOC"))
                        .y(rs.getDouble("Y_LOC"))
                        .angle(rs.getDouble("A_LOC"))
                        .place(true)
                        .build();

                log.debug("Une localisation est chargée: " + obj);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        return Optional.ofNullable(obj);
    }

    @Override
    public Localisation insert(Localisation objet) throws Exception {
        assert objet != null && objet.getElementId() != null : "La localisation doit avoir un elementId";

        try (PreparedStatement ps = connexion.prepareStatement(SQL_INSERT)) {

            ps.setInt(1, objet.getElementId());
            ps.setInt(2, objet.getPiece().getId());
            ps.setDouble(3, objet.getX());
            ps.setDouble(4, objet.getY());
            ps.setDouble(5, objet.getAngle());

            int nb = ps.executeUpdate();

            if (nb == 1) {
                objet.setPlace(true);

                if (!this.connexion.getAutoCommit())
                    this.connexion.commit();

                log.debug("Ajout d'une localisation: " + objet);
            }

        } catch (SQLException e) {
            log.error("Insertion localisation non validée: " + e);

            if (!this.connexion.getAutoCommit())
                this.connexion.rollback();

            this.factory.dispatchException(e, "TLOCALISATION");
        }

        return objet;
    }

    @Override
    public boolean delete(Localisation object) throws Exception {
        assert object != null && object.getElementId() != null : "La localisation doit avoir un elementId";

        boolean ok = false;

        try (PreparedStatement ps = connexion.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, object.getElementId());

            int nb = ps.executeUpdate();
            ok = nb == 1;

            if (ok) {
                object.setPlace(false);

                if (!this.connexion.getAutoCommit())
                    this.connexion.commit();
            }

        } catch (SQLException e) {
            log.error("Suppression localisation non validée: " + e);

            if (!this.connexion.getAutoCommit())
                this.connexion.rollback();

            this.factory.dispatchException(e, "TLOCALISATION");
        }

        return ok;
    }

    @Override
    public boolean update(Localisation object) throws Exception {
        assert object != null && object.getElementId() != null : "La localisation doit avoir un elementId";

        boolean ok = false;

        try (PreparedStatement ps = connexion.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, object.getPiece().getId());
            ps.setDouble(2, object.getX());
            ps.setDouble(3, object.getY());
            ps.setDouble(4, object.getAngle());
            ps.setInt(5, object.getElementId());

            int nb = ps.executeUpdate();
            ok = nb == 1;

            if (ok && !this.connexion.getAutoCommit())
                this.connexion.commit();

            log.debug("Localisation mise à jour: " + object);

        } catch (SQLException e) {
            log.error("Mise à jour localisation non validée: " + e);

            if (!this.connexion.getAutoCommit())
                this.connexion.rollback();

            this.factory.dispatchException(e, "TLOCALISATION");
        }

        return ok;
    }
}
