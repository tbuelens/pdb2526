package org.isfce.pdb.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Plan {
	private Integer id;
	private String nom;
	private String fichier;
	private Installation installation;
}
