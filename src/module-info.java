module pdb2526 {
	requires org.slf4j;
	requires org.firebirdsql.jaybird;
	requires java.sql;
	requires javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
	requires java.base;
	requires lombok;
	requires javafx.base;
	
	// opens org.isfce.pdb.view to javafx.fxml;
	//opens org.isfce.pdb to org.junit.platform.commons;
	
	opens org.isfce.pdb.controller to javafx.fxml,javafx.graphics;
	opens org.isfce.pdb.view.piece to javafx.fxml, javafx.graphics;
	opens org.isfce.pdb.view.plan to javafx.fxml, javafx.graphics;
	opens org.isfce.pdb.view.element to javafx.fxml, javafx.graphics;
	opens org.isfce.pdb.view.installation to javafx.fxml, javafx.graphics;

	exports org.isfce.pdb;
	exports org.isfce.pdb.controller;
	exports org.isfce.pdb.view.piece to  javafx.fxml, javafx.graphics;
	exports org.isfce.pdb.services to javafx.fxml;
	exports org.isfce.pdb.model to javafx.fxml;
	exports org.isfce.pdb.dao to javafx.fxml;
	exports org.isfce.pdb.view.plan to javafx.fxml, javafx.graphics;
	exports org.isfce.pdb.view.element to javafx.fxml, javafx.graphics;
	exports org.isfce.pdb.view.installation to javafx.fxml, javafx.graphics;
	
}