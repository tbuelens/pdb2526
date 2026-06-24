package org.isfce.pdb.view.plan;

import java.net.URL;
import java.util.ResourceBundle;

import org.isfce.pdb.controller.MainController;
import org.isfce.pdb.exceptions.InstallationException;
import org.isfce.pdb.model.Installation;
import org.isfce.pdb.model.Plan;
import org.isfce.pdb.services.Facade;
import org.isfce.pdb.view.bundle.I18N;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur de la vue qui permet d'ajouter un plan (étage). On précise le nom
 * du plan et le numéro de l'installation à laquelle il appartient. Le plan est
 * ensuite rendu persistant en base de données via la façade.
 */
@Slf4j
public class VueAjouterPlanController implements Initializable {
	// pseudo classe pour les erreurs (cohérent avec les autres vues)
	private static final PseudoClass errorClass = PseudoClass.getPseudoClass("error");

	@FXML
	private TextField ztNom;
	@FXML
	private TextField ztInstallation;

	private MainController ctrl;
	private Stage stage;
	private Facade facade;

	@FXML
	void actionAnnuler(ActionEvent event) {
		stage.close();
	}

	@FXML
	void actionValider(ActionEvent event) {
		if (checkData())
			return;
		try {
			String nom = ztNom.getText().trim();
			int numInstallation = Integer.parseInt(ztInstallation.getText().trim());
			// l'installation n'est référencée que par son id (clé étrangère du plan)
			Installation installation = Installation.builder().id(numInstallation).build();
			// crée et rend persistant le plan (l'ID est généré par la BD)
			Plan plan = new Plan(null, nom, nom + ".png", installation);
			facade.insertPlan(plan);
			stage.close();
		} catch (InstallationException e) {
			ctrl.showErreur(I18N.getString(e.getMessage()));
		}
	}

	/**
	 * Vérifie la validité des champs et active la pseudo-classe "error" en
	 * conséquence.
	 *
	 * @return true si les données sont incorrectes
	 */
	private boolean checkData() {
		boolean bad = false;
		boolean erreur;

		// nom obligatoire
		erreur = ztNom.getText().isBlank();
		ztNom.pseudoClassStateChanged(errorClass, erreur);
		bad = bad || erreur;

		// numéro d'installation: entier obligatoire
		erreur = false;
		try {
			Integer.parseInt(ztInstallation.getText().trim());
		} catch (NumberFormatException e) {
			erreur = true;
		}
		ztInstallation.pseudoClassStateChanged(errorClass, erreur);
		bad = bad || erreur;

		return bad;
	}

	/**
	 * Permet de fournir l'accès aux données
	 *
	 * @param ctrl  le contrôleur principal
	 * @param stage la fenêtre de la vue
	 */
	public void setUp(MainController ctrl, Stage stage) {
		this.ctrl = ctrl;
		this.stage = stage;
		this.facade = ctrl.getFacade();

		// pré-remplit le numéro avec l'installation courante si elle est chargée
		try {
			ztInstallation.setText(String.valueOf(facade.getCurrentInstallation().getId()));
		} catch (InstallationException e) {
			// pas d'installation chargée: le champ reste à compléter manuellement
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// rien de particulier à initialiser
	}
}
