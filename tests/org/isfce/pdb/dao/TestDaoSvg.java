package org.isfce.pdb.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Svg;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.*;

public class TestDaoSvg {

    private static ISvgDao dao;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        ConnexionSingleton.setInfoConnexion(
                new ConnexionFromFile("./ressources/connexionPDB2526_test.properties", Databases.FIREBIRD));
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");

        var factory = DAOFactory.getDAOFactory(TypePersistance.FIREBIRD, ConnexionSingleton.getConnexion());
        dao = factory.getSvgDao();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");
        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testGetFromID() {
        var oSvg = dao.getFromID("PC11");
        assertTrue(oSvg.isPresent());

        Svg svg = oSvg.get();
        assertEquals("PC11", svg.id());
        assertEquals(0.0, svg.x());
        assertEquals(-3.1, svg.y());
        assertEquals(5.1, svg.width());
        assertEquals(6.2, svg.height());
    }
}
