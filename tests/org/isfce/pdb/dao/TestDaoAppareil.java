package org.isfce.pdb.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Appareil;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.*;

public class TestDaoAppareil {

    private static IAppareilDao dao;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        ConnexionSingleton.setInfoConnexion(
                new ConnexionFromFile("./ressources/connexionPDB2526_test.properties", Databases.FIREBIRD));
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");

        var factory = DAOFactory.getDAOFactory(TypePersistance.FIREBIRD, ConnexionSingleton.getConnexion());
        dao = factory.getAppareilDao();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");
        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testGetFromID() {
        var oApp = dao.getFromID("PC11");
        assertTrue(oApp.isPresent());

        Appareil app = oApp.get();
        assertEquals("PC11", app.getCode());
        assertEquals("Prise Classique terre/Enf", app.getNom());
        assertEquals(Appareil.Classe.PRISE, app.getClasse());
        assertNotNull(app.getSvg());
        assertEquals("PC11", app.getSvg().id());
    }
}
