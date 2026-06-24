package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.isfce.pdb.model.Adresse;
import org.isfce.pdb.model.Installation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLInstallationDao implements IInstallationDao {
	
	private static String SQL_GET_FROM_ID = """
			select i.NUM_INS, i.DATE_INS, i.ADRESSE_INS, i.CP_INS, i.VILLE_INS, i.INSTALLATEUR_INS, i.PROPRIETAIRE_INS from TINSTALLATION i
			where i.NUM_INS = ?
			""";
	private static String SQL_GET_LISTE = """
			select i.NUM_INS, i.DATE_INS, i.ADRESSE_INS, i.CP_INS, i.VILLE_INS, i.INSTALLATEUR_INS, i.PROPRIETAIRE_INS from TINSTALLATION i
			order by i.DATE_INS DESC
			""";
	private static String SQL_UPDATE = """
	        update TINSTALLATION
	        set DATE_INS = ?,
	            ADRESSE_INS = ?,
	            CP_INS = ?,
	            VILLE_INS = ?,
	            INSTALLATEUR_INS = ?,
	            PROPRIETAIRE_INS = ?
	        where NUM_INS = ?
	        """;
	
	private DAOFactory factory;
	private Connection connexion;

	public SQLInstallationDao(DAOFactory factory) {
		this.factory = factory;
		this.connexion = factory.getConnection();
	}

	@Override
	public Optional<Installation> getFromID(Integer id) {
	    Installation obj = null;

	    try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_FROM_ID)) {

	        ps.setInt(1, id);

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {

	            Adresse adresse = new Adresse(
	                    rs.getString("ADRESSE_INS"),
	                    rs.getInt("CP_INS"),
	                    rs.getString("VILLE_INS")
	            );

	            obj = Installation.builder()
	                    .id(rs.getInt("NUM_INS"))
	                    .date(rs.getDate("DATE_INS").toLocalDate())
	                    .adresse(adresse)
	                    .Installateur(rs.getString("INSTALLATEUR_INS"))
	                    .proprietaire(rs.getString("PROPRIETAIRE_INS"))
	                    .build();

	            log.debug("Une installation est chargée: " + obj);
	        }

	    } catch (SQLException e) {
	        log.error(e.getMessage());
	    }

	    return Optional.ofNullable(obj);
	}

	@Override
	public List<Installation> getListe(String regExpr) {
	    List<Installation> liste = new ArrayList<>(20);

	    try (Statement st = connexion.createStatement()) {

	        ResultSet rs = st.executeQuery(SQL_GET_LISTE);

	        while (rs.next()) {

	            Adresse adresse = new Adresse(
	                    rs.getString("ADRESSE_INS"),
	                    rs.getInt("CP_INS"),
	                    rs.getString("VILLE_INS")
	            );

	            Installation obj = Installation.builder()
	                    .id(rs.getInt("NUM_INS"))
	                    .date(rs.getDate("DATE_INS").toLocalDate())
	                    .adresse(adresse)
	                    .Installateur(rs.getString("INSTALLATEUR_INS"))
	                    .proprietaire(rs.getString("PROPRIETAIRE_INS"))
	                    .build();

	            liste.add(obj);
	        }

	    } catch (SQLException e) {
	        log.error(e.getMessage());
	    }

	    return liste;
	}

	@Override
	public boolean update(Installation object) throws Exception {
	    boolean ok = false;

	    try (PreparedStatement ps = connexion.prepareStatement(SQL_UPDATE)) {

	        ps.setDate(1, Date.valueOf(object.getDate()));

	        ps.setString(2, object.getAdresse().getRue());
	        ps.setInt(3, object.getAdresse().getCp());
	        ps.setString(4, object.getAdresse().getVille());

	        ps.setString(5, object.getInstallateur());
	        ps.setString(6, object.getProprietaire());

	        ps.setInt(7, object.getId());

	        int nbLignes = ps.executeUpdate();

	        ok = nbLignes == 1;

	        if (ok && !this.connexion.getAutoCommit()) {
	            this.connexion.commit();
	        }

	        log.debug("Installation mise à jour: " + object);

	    } catch (SQLException e) {
	        log.error("Mise à jour non validée: " + e.getMessage());

	        if (!this.connexion.getAutoCommit()) {
	            this.connexion.rollback();
	        }

	        this.factory.dispatchException(e, "TINSTALLATION");
	    }

	    return ok;
	}
	
	

}
