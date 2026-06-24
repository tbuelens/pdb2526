package org.isfce.pdb.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.isfce.pdb.model.Svg;

public class CacheSvgDao implements ISvgDao {

    private ISvgDao dao;
    private Map<String, Svg> cache = new HashMap<>();

    public CacheSvgDao(ISvgDao dao) {
        this.dao = dao;
    }

    @Override
    public Optional<Svg> getFromID(String id) {
        String key = id.trim().toUpperCase();

        if (cache.containsKey(key))
            return Optional.of(cache.get(key));

        var oObj = dao.getFromID(key);

        oObj.ifPresent((obj) -> cache.put(key, obj));

        return oObj;
    }
}
