package org.isfce.pdb.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.isfce.pdb.model.TypePiece;

public class CacheTypePieceDao implements ITypePieceDao {
	private boolean firstLoad = true;
	private ITypePieceDao dao;
	private Map<String, TypePiece> cache = new HashMap<String, TypePiece>();

	public CacheTypePieceDao(ITypePieceDao dao) {
		this.dao = dao;
	}

	@Override
	public Optional<TypePiece> getFromID(String id) {
		if (cache.containsKey(id)) {// existe dans le cache?
			return Optional.of(cache.get(id).clone());
		}
		// va chercher l'objet en BD
		var oObj = dao.getFromID(id);
		// rajoute l'objet au cache s'il existe
		oObj.ifPresent((obj) -> cache.put(id, obj));
		return oObj;
	}

	@Override
	public List<TypePiece> getListe(String regExpr) {
		List<TypePiece> liste;
		if (firstLoad) {
			liste = dao.getListe(null);
			liste.forEach((e) -> cache.put(e.getCode(), e));
			firstLoad = false;
		}
		return cache.values().stream().map(e -> e.clone()).toList();
	}

	@Override
	public TypePiece insert(TypePiece objet) throws Exception {
		objet = dao.insert(objet);
		cache.put(objet.getCode(), objet);
		return objet;
	}

}
