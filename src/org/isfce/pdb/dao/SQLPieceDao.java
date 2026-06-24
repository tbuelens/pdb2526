package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.isfce.pdb.model.Piece;
import org.isfce.pdb.model.Plan;
import org.isfce.pdb.model.TypePiece;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLPieceDao implements IPieceDao {
	private static String SQL_GET_FROM_ID = """
	        SELECT NUM_PIE, NOM_PIE, DESCRIPTION_PIE, ETAGE_PIE, FKTYPE_PIE, FKINSTALLATION_PIE, FKPLAN_PIE
	        FROM TPIECE
	        WHERE NUM_PIE = ?
	        """;
	private static String SQL_GET_LISTE_FROM_INST = """
	        SELECT NUM_PIE, NOM_PIE, DESCRIPTION_PIE, ETAGE_PIE, FKTYPE_PIE, FKINSTALLATION_PIE, FKPLAN_PIE
	        FROM TPIECE
	        WHERE FKINSTALLATION_PIE = ?
	        ORDER BY ETAGE_PIE, FKTYPE_PIE
	        """;
	private static String SQL_INSERT = """
	        INSERT INTO TPIECE (NOM_PIE, DESCRIPTION_PIE, ETAGE_PIE, FKTYPE_PIE, FKINSTALLATION_PIE, FKPLAN_PIE)
	        VALUES (?, ?, ?, ?, ?, ?)
	        """;
	private static String SQL_UPDATE = """
			UPDATE TPIECE SET NOM_PIE = ?, DESCRIPTION_PIE = ?, ETAGE_PIE = ?, FKTYPE_PIE = ?
			WHERE NUM_PIE = ?
			""";
	private static String SQL_DELETE = """
			DELETE FROM TPIECE WHERE NUM_PIE=?
			""";
	private DAOFactory factory;
	private Connection connexion;

	public SQLPieceDao(DAOFactory factory) {
		this.factory = factory;
		this.connexion = factory.getConnection();
	}

	@Override
	public Optional<Piece> getFromID(Integer id) {
		Piece obj = null;
		try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_FROM_ID)) {
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String typeP = rs.getString("FKTYPE_PIE");
				TypePiece tp = factory.getTypePieceDAO().getFromID(typeP).get();// A corriger
				// FKPLAN_PIE est facultatif: une pièce peut ne pas (encore) être liée à un plan
				int fkPlan = rs.getInt("FKPLAN_PIE");
				Plan plan = rs.wasNull() ? null
				        : factory.getPlanDao().getFromID(fkPlan).orElse(null);
//@formatter:off 
				obj = Piece.builder()
						.id(id)
						.nom(rs.getString("NOM_PIE"))
						.description(rs.getString("DESCRIPTION_PIE"))
						.etage(rs.getBigDecimal("ETAGE_PIE").setScale(1))
						.typePiece(tp)
						.installation(rs.getInt("FKINSTALLATION_PIE"))
						.plan(plan)
						.build();
//@formatter:on
				log.debug("Une pièce est chargée: " + obj);
			}
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		return Optional.ofNullable(obj);
	}

	@Override
	public Piece insert(Piece obj) throws Exception {
		assert obj != null && obj.getId() == null : "L'objet doit exister sans ID ";
		try (PreparedStatement ps = connexion.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, obj.getNom().trim());
			ps.setString(2, obj.getDescription().trim());
			ps.setBigDecimal(3, obj.getEtage());
			ps.setString(4, obj.getTypePiece().getCode());
			ps.setInt(5, obj.getInstallation());
			// FKPLAN_PIE est facultatif: une pièce peut ne pas (encore) être liée à un plan
			if (obj.getPlan() != null)
				ps.setInt(6, obj.getPlan().getId());
			else
				ps.setNull(6, Types.INTEGER);
			int nb = ps.executeUpdate();
			if (nb == 1) {
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					obj.setId(rs.getInt(1));
					if (!this.connexion.getAutoCommit())
						this.connexion.commit();
				} else
					log.error("L'insert n'a pas retounée l'ID auto généré");
			} else
				log.error("L'insert n'a pas fonctionné");
		} catch (SQLException e) {
			log.error("Insertion non validée: " + e);
			if (!this.connexion.getAutoCommit())
				this.connexion.rollback();
			this.factory.dispatchException(e, "[INS] PIECE");
		}
		return obj;
	}

	@Override
	public List<Piece> getListeFromInstallation(Integer installation) {
		List<Piece> liste = new ArrayList<Piece>();
		Piece obj = null;
		try (PreparedStatement ps = connexion.prepareStatement(SQL_GET_LISTE_FROM_INST)) {
			ps.setInt(1, installation);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String typeP = rs.getString("FKTYPE_PIE");
				TypePiece tp = factory.getTypePieceDAO().getFromID(typeP).get();
				//@formatter:off 
				obj = Piece.builder()
						.id(rs.getInt("NUM_PIE"))
						.nom(rs.getString("NOM_PIE"))
						.description(rs.getString("DESCRIPTION_PIE"))
						.etage(rs.getBigDecimal("ETAGE_PIE").setScale(1))
						.typePiece(tp)
						.installation(installation)
						.build();
				liste.add(obj);
				//@formatter:on
			}
		} catch (SQLException e) {
			log.error("Un problème lors du chargement de la liste des Pièces");
		}
		return liste;
	}
	
	
	

	@Override
	public boolean update(Piece obj) throws Exception {
		boolean ok = false;
		try (PreparedStatement ps = connexion.prepareStatement(SQL_UPDATE)) {
			ps.setString(1,obj.getNom());
			ps.setString(2,obj.getDescription());
			ps.setBigDecimal(3, obj.getEtage());
			ps.setString(4, obj.getTypePiece().getCode());
			ps.setInt(5, obj.getId());
			int nb = ps.executeUpdate();
			if (nb == 1) {
				this.connexion.commit();
				ok = true;
			}
		} catch (SQLException e) {
			log.error("Mise à jour non validée: " + e);
			if (!this.connexion.getAutoCommit())
				this.connexion.rollback();
			this.factory.dispatchException(e, "[UPD] PIECE");
		}

		return ok;
	}

	@Override
	public boolean delete(Piece obj) throws Exception {
		assert obj != null : " L'objet Pièce ne peut pas être à null";
		boolean ok = false;
		try (PreparedStatement ps = connexion.prepareStatement(SQL_DELETE)) {
			ps.setInt(1, obj.getId());
			int nb = ps.executeUpdate();
			if (nb == 1) {
				this.connexion.commit();
				ok = true;
			}
		} catch (SQLException e) {
			log.error("Suppression non validée: " + e);
			if (!this.connexion.getAutoCommit())
				this.connexion.rollback();
			this.factory.dispatchException(e, "[DEL] PIECE");
		}

		return ok;
	}
	
	

}
