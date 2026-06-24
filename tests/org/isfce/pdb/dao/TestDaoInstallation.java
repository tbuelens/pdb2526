package org.isfce.pdb.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Adresse;
import org.isfce.pdb.model.Installation;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.*;

public class TestDaoInstallation {

    private static IInstallationDao dao;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        ConnexionSingleton.setInfoConnexion(
                new ConnexionFromFile("./ressources/connexionPDB2526_test.properties", Databases.FIREBIRD));
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");

        var factory = DAOFactory.getDAOFactory(TypePersistance.FIREBIRD, ConnexionSingleton.getConnexion());
        dao = factory.getInstallationDao();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        DatabaseUtil.executeScriptSQL(ConnexionSingleton.getConnexion(), "./ressources/scriptInitDBTest.sql");
        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testGetFromID() {
        var oIns = dao.getFromID(3);
        assertTrue(oIns.isPresent());

        Installation ins = oIns.get();
        assertEquals(3, ins.getId());
        assertEquals(LocalDate.of(2026, 4, 2), ins.getDate());
        assertEquals(new Adresse("Av Joseph Buedts", 1040, "Etterbeek"), ins.getAdresse());
        assertEquals("ABV", ins.getInstallateur());
        assertEquals("M007", ins.getProprietaire());
    }

    @Test
    void testGetListe() {
        var liste = dao.getListe(null);
        assertEquals(1, liste.size());
        assertEquals(3, liste.get(0).getId());
    }

    @Test
    void testUpdate() throws Exception {
        Installation ins = Installation.builder()
                .id(3)
                .date(LocalDate.of(2026, 5, 10))
                .adresse(new Adresse("Rue Test", 1000, "Bruxelles"))
                .Installateur("XYZ")
                .proprietaire("P999")
                .build();

        assertTrue(dao.update(ins));

        var oIns = dao.getFromID(3);
        assertTrue(oIns.isPresent());
        assertEquals(ins, oIns.get());
    }
}
