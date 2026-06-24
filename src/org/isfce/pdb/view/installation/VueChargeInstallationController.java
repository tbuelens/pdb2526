package org.isfce.pdb.view.installation;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.isfce.pdb.controller.MainController;
import org.isfce.pdb.model.Installation;
import org.isfce.pdb.services.Facade;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur de la vue qui permet de sélectionner une installation parmi la
 * liste des installations existantes (triées de la plus récente à la plus
 * ancienne) et de la charger via la façade.
 */
@Slf4j
public class VueChargeInstallationController implements Initializable {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	@FXML
	private Button btAnnuler;

	@FXML
	private Button btCharger;

	@FXML
	private ListView<Installation> lstInstallations;

	private Stage stage;

	private MainController ctrl;

	private Facade facade;

	@FXML
	void actionAnnuler(ActionEvent event) {
		stage.close();
	}

	@FXML
	void actionCharger(ActionEvent event) {
		Installation installation = lstInstallations.getSelectionModel().getSelectedItem();
		if (installation == null)
			return;
		// délègue le chargement (et la gestion d'erreur) au contrôleur principal
		if (ctrl.chargeInstallation(installation))
			stage.close();
	}

	/**
	 * Permet de fournir l'accès aux données
	 *
	 * @param ctrl  le contrôleur principal
	 * @param stage la fenêtre de la vue
	 */
	public void setUp(MainController ctrl, Stage stage) {
		this.stage = stage;
		this.ctrl = ctrl;
		this.facade = ctrl.getFacade();

		// alimente la liste avec les installations (déjà triées par la couche DAO)
		lstInstallations.setItems(FXCollections.observableArrayList(facade.getListeInstallations()));
		lstInstallations.getSelectionModel().selectFirst();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lstInstallations.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		// charger n'est actif que si une installation est sélectionnée
		btCharger.disableProperty().bind(lstInstallations.getSelectionModel().selectedItemProperty().isNull());

		// affiche une ligne lisible par installation (id, date, ville, propriétaire)
		lstInstallations.setCellFactory(_ -> new ListCell<>() {
			@Override
			protected void updateItem(Installation installation, boolean empty) {
				super.updateItem(installation, empty);
				if (empty || installation == null) {
					setText(null);
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append("N°").append(installation.getId());
					if (installation.getDate() != null)
						sb.append(" - ").append(installation.getDate().format(DATE_FORMAT));
					if (installation.getAdresse() != null)
						sb.append(" - ").append(installation.getAdresse().getVille());
					if (installation.getProprietaire() != null)
						sb.append(" (").append(installation.getProprietaire()).append(")");
					setText(sb.toString());
				}
			}
		});
	}
}
