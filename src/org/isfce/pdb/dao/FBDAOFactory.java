package org.isfce.pdb.dao;

import java.sql.Connection;
import java.sql.SQLException;

import org.isfce.pdb.exceptions.CheckException;
import org.isfce.pdb.exceptions.FKException;
import org.isfce.pdb.exceptions.InstallationException;
import org.isfce.pdb.exceptions.PKException;

public class FBDAOFactory extends DAOFactory {
	private Connection connexion;
	private ITypePieceDao daoTypePiece = null;
	private IPieceDao daoPiece = null;
	private IAppareilDao daoAppareil = null;
	private ISvgDao daoSvg = null;
	private IElementDao daoElement = null;
	private IInstallationDao daoInstallation = null;
	private IPlanDao daoPlan = null;
	private ILocalisationDao daoLocalisation = null;

	public FBDAOFactory(Connection connexion) {
		this.connexion = connexion;
	}

	@Override
	public ITypePieceDao getTypePieceDAO() {
		if (daoTypePiece == null)
			daoTypePiece = new CacheTypePieceDao( new SQLTypePieceDao(this));
		return daoTypePiece;
	}

	@Override
	public IPieceDao getPieceDAO() {
		if (daoPiece == null)
			daoPiece = new SQLPieceDao(this);
		return daoPiece;
	}
	
	@Override
	public IAppareilDao getAppareilDao() {
		if (daoAppareil == null)
			daoAppareil = new CacheAppareilDao( new SQLAppareilDao(this));
		return daoAppareil;
	}

	@Override
	public ISvgDao getSvgDao() {
		if (daoSvg == null)
			daoSvg = new CacheSvgDao( new SQLSvgDao(this));
		return daoSvg;
	}

	@Override
	public IInstallationDao getInstallationDao() {
		if (daoInstallation == null)
			daoInstallation = new SQLInstallationDao(this);
		return daoInstallation;
	}

	@Override
	public IElementDao getElementDao() {
		if (daoElement == null)
			daoElement = new SQLElementDao(this);
		return daoElement;
	}
	
	@Override
	public IPlanDao getPlanDao() {
		if (daoPlan == null)
			daoPlan = new SQLPlanDao(this);
		return daoPlan;
	}

	@Override
	public ILocalisationDao getLocalisationDao() {
		if (daoLocalisation == null)
			daoLocalisation = new SQLLocalisationDao(this);
		return daoLocalisation;
	}
	
	@Override
	public Connection getConnection() {
		return connexion;
	}

	@Override
	protected void dispatchException(Exception e, String detail) throws InstallationException {
		SQLException exc = (SQLException) e;

		throw switch (exc.getErrorCode()) {
		case 335544665 -> new PKException(e.getMessage(), detail);
		case 335544347 -> new CheckException(e.getMessage(), detail);
		case 335544466 -> {//Clé étrangère non valide
			String txt;
			String err;
			if (detail.startsWith("[DEL]")) {
				err = "err.suppression.impossible";
				txt = detail.substring(5);
			} else if (detail.startsWith("[INS]")) {
				err = "err.insertion.impossible";
				txt = detail.substring(5);
			} else if (detail.startsWith("[UPD]")) {
				err = "err.mise_a_jour.impossible";
				txt = detail.substring(5);
			} else {
				err = "err.insertion.impossible";
				txt = detail;
			}
			yield new FKException(err, txt);
		}

		default -> new InstallationException(" Problème " + exc);

		};

	}

}
