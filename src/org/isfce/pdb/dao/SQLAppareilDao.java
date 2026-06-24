package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.isfce.pdb.model.Appareil;
import org.isfce.pdb.model.Svg;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLAppareilDao implements IAppareilDao {
	
	private static String SQL_GET_FROM_ID="""
			select a.CODE_APP, a.NOM_APP, a.FKSVG_APP, a.CLASSE_APP from TAPPAREIL a
			where a.CODE_APP = ?
			""";
	
	private DAOFactory factory; 
	private Connection connexion;
	

	public SQLAppareilDao(DAOFactory factory) {
		this.factory = factory;
		this.connexion = factory.getConnection();
	}

	@Override
	public Optional<Appareil> getFromID(String id) {
	    Appareil obj = null;

	    try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_FROM_ID)) {

	        ps.setString(1, id.trim().toUpperCase());

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {
	            String svgId = rs.getString("FKSVG_APP");
	            Svg svg = factory.getSvgDao().getFromID(svgId).get();

	            Appareil.Classe classe = Appareil.Classe.valueOf(
	                    rs.getString("CLASSE_APP").trim().toUpperCase()
	            );

	            obj = new Appareil(
	                    rs.getString("CODE_APP").trim(),
	                    rs.getString("NOM_APP").trim(),
	                    svg,
	                    classe
	            );

	            log.debug("Un appareil est chargé: " + obj);
	        }

	    } catch (SQLException e) {
	        log.error(e.getMessage());
	    }

	    return Optional.ofNullable(obj);
	}

}
