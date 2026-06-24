package org.isfce.pdb.view.element;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.isfce.pdb.controller.MainController;
import org.isfce.pdb.model.Appareil;
import org.isfce.pdb.model.Element;
import org.isfce.pdb.model.Piece;
import org.isfce.pdb.services.Facade;
import org.isfce.pdb.services.ListMessages;
import org.isfce.pdb.services.ListMessages.Classe;
import org.isfce.pdb.services.TypeOperation;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur de la vue qui affiche la liste des éléments (table TELEMENT) d'une
 * installation. Via la colonne "Pièce" (une ComboBox), on peut associer chaque
 * élément à une pièce afin de créer/modifier sa localisation.
 *
 * La vue s'abonne aux évènements de publication de la façade pour se rafraîchir
 * lorsqu'une pièce est ajoutée ou supprimée.
 */
@Slf4j
public class VueListeElementController implements Initializable, Subscriber<ListMessages> {

	@FXML
	private Button btQuitter;

	@FXML
	private Button btRecharger;

	@FXML
	private TableColumn<Element, Integer> colId;

	@FXML
	private TableColumn<Element, Appareil> colAppareil;

	@FXML
	private TableColumn<Element, Integer> colQt;

	@FXML
	private TableColumn<Element, String> colCode;

	@FXML
	private TableColumn<Element, String> colInfo;

	@FXML
	private TableColumn<Element, Integer> colOrdre;

	@FXML
	private TableColumn<Element, Piece> colPiece;

	@FXML
	private TableView<Element> tblElements;

	private Stage stage;

	private MainController ctrl;

	private Facade facade;

	// abonnement au publisher de la façade
	private Subscription subscription;

	// liste observable des éléments affichés dans le tableau
	private ObservableList<Element> obsElements = FXCollections.observableArrayList();

	// liste observable des pièces pour la ComboBox d'association
	private ObservableList<Piece> obsPieces = FXCollections.observableArrayList();

	@FXML
	void actionQuitter(ActionEvent event) {
		fermeture();
		stage.close();
	}

	@FXML
	void actionRecharger(ActionEvent event) {
		chargeDonnees();
	}

	/**
	 * Action à exécuter lorsqu'on quitte la vue : on se désabonne du publisher
	 */
	private void fermeture() {
		if (subscription != null)
			subscription.cancel();
		obsElements.clear();
		obsPieces.clear();
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

		chargeDonnees();

		// s'abonne aux évènements du modèle
		facade.addObserver(this);

		// se désabonne si on ferme via la croix
		stage.setOnCloseRequest(_ -> fermeture());
	}

	/**
	 * Charge (ou recharge) la liste des éléments et la liste des pièces
	 */
	private void chargeDonnees() {
		// liste des pièces pour la ComboBox
		obsPieces.setAll(facade.getListePieces());
		// liste des éléments
		List<Element> listeElements = facade.getListeElements();
		obsElements.setAll(listeElements);
		tblElements.setItems(obsElements);
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// Champs de la table TELEMENT (toutes les colonnes sont en lecture seule)
		colId.setCellValueFactory(p -> new ReadOnlyObjectWrapper<Integer>(p.getValue().getId()));
		colAppareil.setCellValueFactory(p -> new ReadOnlyObjectWrapper<Appareil>(p.getValue().getAppareil()));
		colQt.setCellValueFactory(p -> new ReadOnlyObjectWrapper<Integer>(p.getValue().getQt()));
		colCode.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getCode()));
		colInfo.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getInfo()));
		colOrdre.setCellValueFactory(p -> new ReadOnlyObjectWrapper<Integer>(p.getValue().getOrdre()));

		// affiche le code et le nom de l'appareil dans la colonne Appareil
		colAppareil.setCellFactory(_ -> new javafx.scene.control.TableCell<>() {
			@Override
			protected void updateItem(Appareil app, boolean empty) {
				super.updateItem(app, empty);
				setText(empty || app == null ? null : app.getCode() + " - " + app.getNom());
			}
		});

		// Colonne Pièce : ComboBox permettant d'associer l'élément à une pièce
		colPiece.setCellValueFactory(p -> {
			var loc = p.getValue().getLocalisation();
			return new ReadOnlyObjectWrapper<Piece>(loc == null ? null : loc.getPiece());
		});

		tblElements.setEditable(true);
		colPiece.setEditable(true);
		colPiece.setCellFactory(ComboBoxTableCell.forTableColumn(new StringConverter<Piece>() {

			@Override
			public Piece fromString(String str) {
				return obsPieces.stream().filter(pc -> pc.getNom().equals(str)).findFirst().orElse(null);
			}

			@Override
			public String toString(Piece piece) {
				return piece == null ? "" : piece.getNom();
			}
		}, obsPieces));

		// Lors du choix d'une pièce, on crée/met à jour la localisation de l'élément
		colPiece.setOnEditCommit(e -> {
			Element element = e.getRowValue();
			Piece piece = e.getNewValue();
			if (piece != null) {
				facade.associeElementPiece(element, piece);
				log.info("Element " + element.getCode() + " associé à la pièce " + piece.getNom());
			}
		});
	}

	/***************************************************************
	 * Gestion des évènements de publication (Subscriber)
	 **************************************************************/

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		// on demande à recevoir les évènements un par un
		subscription.request(1);
	}

	@Override
	public void onNext(ListMessages item) {
		// La vue réagit aux ajouts/suppressions de pièces pour mettre à jour la
		// ComboBox d'association. Le rafraîchissement se fait sur le thread JavaFX.
		if (item.hasEventFromClasse(Classe.PIECE)) {
			Platform.runLater(() -> {
				item.getEventFromClasse(Classe.PIECE).ifPresent(ev -> {
					Piece piece = (Piece) ev.element();
					if (ev.op() == TypeOperation.INSERT) {
						if (!obsPieces.contains(piece))
							obsPieces.add(piece);
					} else if (ev.op() == TypeOperation.DELETE) {
						obsPieces.remove(piece);
					}
				});
				tblElements.refresh();
			});
		}
		// demande l'évènement suivant
		if (subscription != null)
			subscription.request(1);
	}

	@Override
	public void onError(Throwable throwable) {
		log.error("Erreur lors de la réception d'un évènement: " + throwable.getMessage());
	}

	@Override
	public void onComplete() {
		log.info("Plus d'évènements à recevoir pour la vue liste des éléments");
	}

}
