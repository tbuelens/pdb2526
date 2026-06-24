package org.isfce.pdb.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Piece;
import org.isfce.pdb.model.Plan;
import org.isfce.pdb.model.TypePiece;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestDaoPiece {

    private static IPieceDao dao;
    private static IPlanDao planDao;

    static TypePiece tp = new TypePiece("SALON", "Salon", false);

    static Plan plan;

    static Piece p;
    static Piece pn;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        ConnexionSingleton.setInfoConnexion(
                new ConnexionFromFile(
                        "./ressources/connexionPDB2526_test.properties",
                        Databases.FIREBIRD));

        // Réinitialise la base de données
        DatabaseUtil.executeScriptSQL(
                ConnexionSingleton.getConnexion(),
                "./ressources/scriptInitDBTest.sql");

        var factory = DAOFactory.getDAOFactory(
                TypePersistance.FIREBIRD,
                ConnexionSingleton.getConnexion());

        dao = factory.getPieceDAO();
        planDao = factory.getPlanDao();

        // Charge le plan utilisé par les pièces
        plan = planDao.getFromID(1).get();

        p = Piece.builder()
                .id(1)
                .nom("SALON")
                .description("Salon principal")
                .etage(new BigDecimal("0.0").setScale(1))
                .typePiece(tp)
                .installation(3)
                .plan(plan)
                .build();

        pn = Piece.builder()
                .nom("SALON2")
                .description("Salon principal2")
                .etage(new BigDecimal("1.0").setScale(1))
                .typePiece(tp)
                .installation(3)
                .plan(plan)
                .build();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {

        DatabaseUtil.executeScriptSQL(
                ConnexionSingleton.getConnexion(),
                "./ressources/scriptInitDBTest.sql");

        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testGetFromId() throws SQLException {

        var oObj = dao.getFromID(1);

        assertTrue(oObj.isPresent());
        assertEquals(p, oObj.get());
    }

    @Test
    void testInsert() throws Exception {

        Piece pn2 = dao.insert(pn);

        assertNotNull(pn2.getId());

        // Vérification dans la base
        var oObj = dao.getFromID(pn2.getId());

        assertTrue(oObj.isPresent());
        assertEquals(pn2, oObj.get());

        // Impossible de réinsérer un objet déjà sauvegardé
        assertThrows(
                AssertionError.class,
                () -> dao.insert(pn2));
    }
}
