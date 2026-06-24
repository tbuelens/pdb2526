package org.isfce.pdb.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Installation;
import org.isfce.pdb.model.Plan;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.*;

public class TestDaoPlan {

    private static IPlanDao dao;
    private static IInstallationDao installationDao;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        ConnexionSingleton.setInfoConnexion(
                new ConnexionFromFile("./ressources/connexionPDB2526_test.properties", Databases.FIREBIRD));

        DatabaseUtil.executeScriptSQL(
                ConnexionSingleton.getConnexion(),
                "./ressources/scriptInitDBTest.sql");

        var factory = DAOFactory.getDAOFactory(
                TypePersistance.FIREBIRD,
                ConnexionSingleton.getConnexion());

        dao = factory.getPlanDao();
        installationDao = factory.getInstallationDao();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        DatabaseUtil.executeScriptSQL(
                ConnexionSingleton.getConnexion(),
                "./ressources/scriptInitDBTest.sql");

        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testGetFromID() {
        var oPlan = dao.getFromID(1);

        assertTrue(oPlan.isPresent());

        Plan plan = oPlan.get();

        assertEquals(1, plan.getId());
        assertEquals("rez", plan.getNom());
        assertEquals("rez.png", plan.getFichier());
        assertEquals(3, plan.getInstallation().getId());
    }

    @Test
    void testGetListePlanFromInstallation() {
        var liste = dao.getListePlanFromInstallation(3);

        assertEquals(2, liste.size());

        Plan planRez = liste.get(0);
        assertEquals(1, planRez.getId());
        assertEquals("rez", planRez.getNom());
        assertEquals("rez.png", planRez.getFichier());
        assertEquals(3, planRez.getInstallation().getId());

        Plan planEtage = liste.get(1);
        assertEquals(2, planEtage.getId());
        assertEquals("etage1", planEtage.getNom());
        assertEquals("etage1.png", planEtage.getFichier());
        assertEquals(3, planEtage.getInstallation().getId());
    }

    @Test
    void testInsert() throws Exception {
        Installation installation = installationDao.getFromID(3).orElseThrow();

        Plan plan = new Plan(
                null,
                "etage2",
                "etage2.png",
                installation
        );

        Plan inserted = dao.insert(plan);

        assertNotNull(inserted.getId());

        var oPlan = dao.getFromID(inserted.getId());
        assertTrue(oPlan.isPresent());

        assertEquals(inserted, oPlan.get());

        assertThrows(AssertionError.class, () -> dao.insert(inserted));
    }
}
