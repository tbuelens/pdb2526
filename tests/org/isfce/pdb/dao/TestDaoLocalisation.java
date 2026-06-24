package org.isfce.pdb.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.isfce.pdb.dao.DAOFactory.TypePersistance;
import org.isfce.pdb.databases.connexion.ConnexionFromFile;
import org.isfce.pdb.databases.connexion.ConnexionSingleton;
import org.isfce.pdb.databases.uri.Databases;
import org.isfce.pdb.model.Localisation;
import org.isfce.pdb.model.Piece;
import org.isfce.pdb.util.DatabaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDaoLocalisation {

    private static ILocalisationDao dao;
    private static IPieceDao pieceDao;

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

        dao = factory.getLocalisationDao();
        pieceDao = factory.getPieceDAO();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        DatabaseUtil.executeScriptSQL(
                ConnexionSingleton.getConnexion(),
                "./ressources/scriptInitDBTest.sql");

        ConnexionSingleton.liberationConnexion();
    }

    @Test
    void testInsertGetUpdateDelete() throws Exception {
        Piece piece = pieceDao.getFromID(1).orElseThrow();

        Localisation loc = Localisation.builder()
                .elementId(79)
                .piece(piece)
                .x(10.5)
                .y(20.5)
                .angle(45.0)
                .place(false)
                .build();

        Localisation inserted = dao.insert(loc);
        assertTrue(inserted.isPlace());

        var oLoc = dao.getFromID(79);
        assertTrue(oLoc.isPresent());

        assertEquals(79, oLoc.get().getElementId());
        assertEquals(piece.getId(), oLoc.get().getPiece().getId());
        assertEquals(10.5, oLoc.get().getX());
        assertEquals(20.5, oLoc.get().getY());
        assertEquals(45.0, oLoc.get().getAngle());
        assertTrue(oLoc.get().isPlace());

        Localisation updated = Localisation.builder()
                .elementId(79)
                .piece(piece)
                .x(30.0)
                .y(40.0)
                .angle(90.0)
                .place(true)
                .build();

        assertTrue(dao.update(updated));

        var oUpdated = dao.getFromID(79);
        assertTrue(oUpdated.isPresent());

        assertEquals(30.0, oUpdated.get().getX());
        assertEquals(40.0, oUpdated.get().getY());
        assertEquals(90.0, oUpdated.get().getAngle());

        assertTrue(dao.delete(updated));
        assertFalse(updated.isPlace());
        assertTrue(dao.getFromID(79).isEmpty());
    }
}