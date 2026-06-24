package org.isfce.pdb.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Element;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.*;

public class TestDaoElement {

    private static IElementDao dao;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        ConnexionSingleton.setInfoConnexion(
                new ConnexionFromFile("./ressources/connexionPDB2526_test.properties", Databases.FIREBIRD));
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");

        var factory = DAOFactory.getDAOFactory(TypePersistance.FIREBIRD, ConnexionSingleton.getConnexion());
        dao = factory.getElementDao();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");
        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testGetListeFromInstallation() {
        var liste = dao.getListeFromInstallation(3);

        assertEquals(79, liste.size());

        Element e1 = liste.stream()
                .filter(e -> e.getId().equals(1))
                .findFirst()
                .orElseThrow();

        assertEquals("PC11", e1.getAppareil().getCode());
        assertEquals(1, e1.getQt());
        assertEquals("A1", e1.getCode());
        assertEquals("cuisine", e1.getInfo());
        assertEquals(0, e1.getOrdre());
    }
}
