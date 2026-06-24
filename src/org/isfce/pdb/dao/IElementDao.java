package org.isfce.pdb.dao;

import java.util.List;

import org.isfce.pdb.model.Element;

public interface IElementDao extends IDAO<Element, Integer> {

    List<Element> getListeFromInstallation(int installation);

}
