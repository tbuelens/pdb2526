package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.isfce.pdb.model.Installation;
import org.isfce.pdb.model.Plan;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLPlanDao implements IPlanDao {

    private static String SQL_GET_FROM_ID = """
            select p.ID_PLA, p.NOM_PLA, p.FKINSTALLATION_PLA
            from TPLAN p
            where p.ID_PLA = ?
            """;

    private static String SQL_GET_LISTE_PLAN_FROM_INSTALLATION = """
            select p.ID_PLA, p.NOM_PLA, p.FKINSTALLATION_PLA
            from TPLAN p
            where p.FKINSTALLATION_PLA = ?
            order by p.ID_PLA
            """;

    private static String SQL_INSERT = """
            insert into TPLAN (NOM_PLA, FKINSTALLATION_PLA)
            values (?, ?)
            """;

    private DAOFactory factory;
    private Connection connexion;

    public SQLPlanDao(DAOFactory factory) {
        this.factory = factory;
        this.connexion = factory.getConnection();
    }

    @Override
    public Optional<Plan> getFromID(Integer id) {
        Plan obj = null;

        try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_FROM_ID)) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Installation ins = factory.getInstallationDao()
                        .getFromID(rs.getInt("FKINSTALLATION_PLA"))
                        .get();

                obj = new Plan(
                        rs.getInt("ID_PLA"),
                        rs.getString("NOM_PLA"),
                        rs.getString("NOM_PLA") + ".png",
                        ins
                );

                log.debug("Un plan est chargé: " + obj);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        return Optional.ofNullable(obj);
    }

    @Override
    public List<Plan> getListePlanFromInstallation(int installation) {
        List<Plan> liste = new ArrayList<>(20);

        try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_LISTE_PLAN_FROM_INSTALLATION)) {

            ps.setInt(1, installation);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Installation ins = factory.getInstallationDao()
                        .getFromID(rs.getInt("FKINSTALLATION_PLA"))
                        .get();

                Plan plan = new Plan(
                        rs.getInt("ID_PLA"),
                        rs.getString("NOM_PLA"),
                        rs.getString("NOM_PLA") + ".png",
                        ins
                );

                liste.add(plan);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        return liste;
    }

    @Override
    public Plan insert(Plan obj) throws Exception {
        assert obj != null && obj.getId() == null : "L'objet doit exister sans ID";

        try (PreparedStatement ps = connexion.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, obj.getNom().trim());
            ps.setInt(2, obj.getInstallation().getId());

            int nb = ps.executeUpdate();

            if (nb == 1) {
                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {
                    obj.setId(rs.getInt(1));

                    if (!this.connexion.getAutoCommit())
                        this.connexion.commit();

                    log.debug("Ajout d'un plan: " + obj);
                } else {
                    log.error("L'insert n'a pas retourné l'ID auto généré");
                }
            } else {
                log.error("L'insert n'a pas fonctionné");
            }

        } catch (SQLException e) {
            log.error("Insertion non validée: " + e);

            if (!this.connexion.getAutoCommit())
                this.connexion.rollback();

            this.factory.dispatchException(e, "TPLAN");
        }

        return obj;
    }
}
