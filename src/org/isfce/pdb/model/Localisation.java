package org.isfce.pdb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Localisation {
	private Integer elementId;
	private Piece piece;
	private double x;
	private double y;
	private double angle;
	private boolean place;
}
