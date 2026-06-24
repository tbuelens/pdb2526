package org.isfce.pdb.dao;

import java.util.List;

import org.isfce.pdb.model.Plan;

public interface IPlanDao extends IDAO<Plan, Integer> {
	
	List<Plan> getListePlanFromInstallation(int installation);

}
