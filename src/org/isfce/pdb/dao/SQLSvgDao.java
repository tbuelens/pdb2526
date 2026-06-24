package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.isfce.pdb.model.Svg;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLSvgDao implements ISvgDao {
	
	private static String SQL_GET_FROM_ID="""
			select s.CODE_SVG, s.SVG_SVG, s.SVGX_SVG, s.SVGY_SVG, s.SVGWIDTH_SVG, s.SVGHEIGHT_SVG from TSVG s
			where s.CODE_SVG = ?
			""";
	
	private DAOFactory factory; 
	private Connection connexion;
	

	public SQLSvgDao(DAOFactory factory) {
		this.factory = factory;
		this.connexion = factory.getConnection();
	}


	@Override
	public Optional<Svg> getFromID(String id) {
	    Svg obj = null;

	    try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_FROM_ID)) {

	        ps.setString(1, id.trim().toUpperCase());

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {
	            obj = new Svg(
	                    rs.getString("CODE_SVG").trim(),
	                    rs.getString("SVG_SVG"),
	                    rs.getDouble("SVGX_SVG"),
	                    rs.getDouble("SVGY_SVG"),
	                    rs.getDouble("SVGWIDTH_SVG"),
	                    rs.getDouble("SVGHEIGHT_SVG")
	            );

	            log.debug("Un SVG est chargé: " + obj);
	        }

	    } catch (SQLException e) {
	        log.error(e.getMessage());
	    }

	    return Optional.ofNullable(obj);
	}
	
	

}
