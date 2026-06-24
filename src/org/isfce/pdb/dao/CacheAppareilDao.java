package org.isfce.pdb.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.isfce.pdb.model.Appareil;

public class CacheAppareilDao implements IAppareilDao {

    private IAppareilDao dao;
    private Map<String, Appareil> cache = new HashMap<>();

    public CacheAppareilDao(IAppareilDao dao) {
        this.dao = dao;
    }

    @Override
    public Optional<Appareil> getFromID(String id) {
        String key = id.trim().toUpperCase();

        if (cache.containsKey(key))
            return Optional.of(cache.get(key));

        var oObj = dao.getFromID(key);

        oObj.ifPresent((obj) -> cache.put(key, obj));

        return oObj;
    }
}
