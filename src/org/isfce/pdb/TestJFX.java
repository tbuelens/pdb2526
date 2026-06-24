package org.isfce.pdb;

import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestJFX extends Application {
	
	@Override
	public void init() throws Exception {
		log.info("LANCEMENT DE L'APPLICATION");
	}

	@Override
	public void stop() throws Exception {
		log.info("BYE BYE");
	}

	@Override
	public void start(Stage primaryStage) {
		
		primaryStage.initStyle(StageStyle.DECORATED);
		
		BorderPane cp = new BorderPane();
		
		Button bt1 = new Button("TEST");
		bt1.setOnAction((_) -> Platform.exit());
		
		Button bt2 = new Button("STAGE");
		bt2.setOnAction((_) -> fenetre());
		
		Button bt3 = new Button("INFO");
		bt3.setOnAction((_) -> {
			var al1 = new Alert(AlertType.INFORMATION);
			al1.setTitle("Test alert");
			al1.setHeaderText("C'est bientot la pause !");
			al1.showAndWait();
		});
		
		Button bt4 = new Button("INFO");
		bt4.setOnAction((_) -> {
			var al1 = new TextInputDialog();
			al1.setTitle("Input");
			al1.setHeaderText("Entrez votre nom:");
			
			Optional<String> oRes = al1.showAndWait();
			oRes.ifPresent((n) -> {
				System.out.println("Hello: " + n);
			});
		});
		
		cp.setCenter(bt1);
		FlowPane basPane = new FlowPane();
		basPane.getChildren().addAll(bt2,bt3,bt4);
		cp.setBottom(basPane);
		
		Scene scene = new Scene(cp, 400, 300);
		
		primaryStage.setScene(scene);
		primaryStage.show();
		
	}
	
	private void fenetre() {
		Stage st1 = new Stage();
		st1.initModality(Modality.WINDOW_MODAL);
		st1.setScene(new Scene(new Pane(), 100, 200));
		st1.showAndWait();
	}

	public static void main(String[] args) {
		launch(args);
	}

	
	
	
}
