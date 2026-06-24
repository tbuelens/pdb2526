package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.isfce.pdb.model.Appareil;
import org.isfce.pdb.model.Element;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLElementDao implements IElementDao {

    private static String SQL_GET_LISTE_FROM_INSTALLATION = """
            select e.ID_ELE, e.FKAPPAREIL_ELE, e.QT_ELE, e.CODE_ELE, e.INFO_ELE, e.ORDRE_ELE
            from TELEMENT e
            where e.FKINSTALLATION_ELE = ?
            order by e.ORDRE_ELE
            """;

    private DAOFactory factory;
    private Connection connexion;

    public SQLElementDao(DAOFactory factory) {
        this.factory = factory;
        this.connexion = factory.getConnection();
    }

    @Override
    public List<Element> getListeFromInstallation(int installation) {
        List<Element> liste = new ArrayList<>(20);

        try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_LISTE_FROM_INSTALLATION)) {

            ps.setInt(1, installation);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String codeAppareil = rs.getString("FKAPPAREIL_ELE");

                Appareil appareil = factory.getAppareilDao()
                        .getFromID(codeAppareil)
                        .get();

                Element element = Element.builder()
                        .id(rs.getInt("ID_ELE"))
                        .appareil(appareil)
                        .qt(rs.getInt("QT_ELE"))
                        .code(rs.getString("CODE_ELE"))
                        .info(rs.getString("INFO_ELE"))
                        .ordre(rs.getInt("ORDRE_ELE"))
                        .build();

                liste.add(element);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        return liste;
    }
}
