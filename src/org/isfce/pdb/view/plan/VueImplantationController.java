package org.isfce.pdb.view.plan;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.isfce.pdb.controller.MainController;
import org.isfce.pdb.exceptions.InstallationException;
import org.isfce.pdb.model.Element;
import org.isfce.pdb.model.Piece;
import org.isfce.pdb.model.Plan;
import org.isfce.pdb.services.Facade;
import org.isfce.pdb.services.ListMessages;
import org.isfce.pdb.services.ListMessages.Classe;
import org.isfce.pdb.services.TypeOperation;
import org.isfce.pdb.view.bundle.I18N;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VueImplantationController implements Initializable, Subscriber<ListMessages> {

	@FXML
	private Button btAnnuler;

	@FXML
	private Button btValider;

	@FXML
	private Canvas canvas;

	@FXML
	private ScrollPane scpCanvas;

	@FXML
	private Group grpPane;

	@FXML
	private Pane pane;

	@FXML
	private ComboBox<Plan> cbPlans;

	// ComboBox de filtrage de la liste des éléments par pièce
	@FXML
	private ComboBox<Piece> cbPieces;

	@FXML
	private ListView<Element> lstElements;

	/* pas besoin pour l'instant */
	private Stage stage;
	private MainController ctrl;

	private Facade facade;

	// abonnement au publisher de la façade
	private Subscription subscription;

	// liste filtrée des éléments du plan courant (filtre par pièce)
	private FilteredList<Element> filteredElements;

	private Optional<Plan> oPlanCharge = Optional.empty();
	// associe à un plan son canvas
	private Map<Integer, Canvas> mapPlanCanvas = new HashMap<>();
	// associe à un plan ses ElementView
	private Map<Integer, List<ElementView>> mapPlanNodes = new HashMap<>();
	// associe à un plan la liste observable de ses éléments
	private Map<Integer, ObservableList<Element>> mapPlanElements = new HashMap<>();

	@FXML
	void actionQuitter(ActionEvent event) {
		//TODO  à ajuster conf.quitter.sans.sauver
		if (ctrl.showConfirmation(I18N.getString("conf.quitter.sans.sauver")))
			stage.hide();
	}

	@FXML
	void actionValider(ActionEvent event) {
		// Sauvegarde des modifications (localisations) via la façade
		if (ctrl.showConfirmation(I18N.getString("conf.save"))) {
			try {
				facade.sauvegarderLocalisations();
				stage.hide();
			} catch (InstallationException e) {
				ctrl.showErreur(I18N.getString("err.save"));
				log.error("Sauvegarde des localisations impossible: " + e.getMessage());
			}
		}
	}

	@FXML
	void actionChargePlan(ActionEvent event) {

		// Charge le plan sélectionné
		oPlanCharge = Optional.ofNullable(cbPlans.getValue());
		if (oPlanCharge.isPresent()) {
			Plan planCharge = oPlanCharge.get();
			// charge le canvas du plan
			canvas = mapPlanCanvas.get(planCharge.getId());

			pane.getChildren().clear();// vide le Pane
			pane.getChildren().add(canvas);// place le dessin de l'étage
			// ajuste le canvas à la taille de l'image
			pane.setPrefSize(canvas.getWidth(), canvas.getHeight());

			// On spécifie les éléments à la "List" lstElements via une liste filtrable
			// (filtre par pièce). Le filtre s'applique sur la liste observable du plan.
			filteredElements = new FilteredList<>(mapPlanElements.get(planCharge.getId()), _ -> true);
			lstElements.setItems(filteredElements);

			// Alimente la ComboBox de filtrage avec les pièces du plan
			// (la première entrée "null" représente "toutes les pièces")
			ObservableList<Piece> obsPieces = FXCollections.observableArrayList();
			obsPieces.add(null);
			obsPieces.addAll(facade.getPiecesPlan(planCharge));
			cbPieces.setItems(obsPieces);
			cbPieces.getSelectionModel().selectFirst();// "toutes les pièces"
			appliquerFiltrePiece(null);

			// dessine les éléments déjà placés
			pane.getChildren().addAll(mapPlanNodes.get(planCharge.getId()));

		}

	}

	/**
	 * Applique le filtre de la liste des éléments en fonction de la pièce
	 * sélectionnée. Une pièce à null signifie "toutes les pièces".
	 *
	 * @param piece la pièce de filtrage (ou null pour tout afficher)
	 */
	private void appliquerFiltrePiece(Piece piece) {
		if (filteredElements == null)
			return;
		if (piece == null)
			filteredElements.setPredicate(_ -> true);
		else
			filteredElements.setPredicate(
					e -> e.getLocalisation() != null && piece.equals(e.getLocalisation().getPiece()));
	}

	/**
	 * Permet de fournir l'accès aux données
	 * 
	 * @param ctrl
	 * @param stage
	 */
	public void setUp(MainController ctrl, Stage stage) {
		this.stage = stage;
		this.ctrl = ctrl;
		this.facade = ctrl.getFacade();

		// liste des plans
		List<Plan> listePlan = facade.getListePlans();
		// liste observable des Plans valide (avec une image), pour la comboBox
		ObservableList<Plan> obsPlan = FXCollections.observableArrayList();
		// Crée la liste des images dans des canvas (couche de fond)
		mapPlanCanvas.clear();
		String basePath;// chemin de base des images
		try {
			// format de base: chemin/installationId/
			basePath = facade.getProperties().getProperty("imagesPath") + facade.getCurrentInstallation().getId() + "/";

			// Crée les canvas pour les plans qui possèdent un fichier lisible
			for (Plan p : listePlan) {
				Path path = Path.of(basePath + p.getFichier());
				if (Files.exists(path)) {
					InputStream stream;
					try {
						stream = Files.newInputStream(path);
						Image image = new Image(stream);
						// Crée un canvas de la taille de l'image
						Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
						canvas.getGraphicsContext2D().drawImage(image, 0, 0);
						mapPlanCanvas.put(p.getId(), canvas);
						obsPlan.add(p);
					} catch (IOException e) {
						ctrl.showErreur("Fichier introuvable: " + p.getFichier());
						log.error(p.getFichier());
						cbPlans.getItems().remove(p);
					}
				}
			}
			// mets la liste observable de la comboBox cbPlans
			cbPlans.setItems(obsPlan);

			// Crée une Map qui associe à chaque plan sa liste observable
			// d'éléments
			obsPlan.forEach(plan -> mapPlanElements.put(plan.getId(),
					FXCollections.observableArrayList(facade.getElementsPlan(plan))));

			// Pour les éléments qui ont une localisation,
			// on crée la map qui associe au plan une liste d'ElementView
			for (Plan plan : obsPlan) {
				List<ElementView> nodes = new ArrayList<ElementView>();
				//pour chaque élément du plan
				for (Element element : mapPlanElements.get(plan.getId()))
					//pour chaque élement déjà placé, on crée un ElementView
					if (element.getLocalisation().isPlace())
						nodes.add(creeElementView(element));
				// pour un plan on associe sa liste d'ElementView
				mapPlanNodes.put(plan.getId(), nodes);
			}
			//Action sur la touche R (rotation) et DEL (suppression)
			stage.getScene().setOnKeyPressed(event -> {
				Element elem = lstElements.getSelectionModel().getSelectedItem();
				if (elem == null || elem.getLocalisation() == null || !elem.getLocalisation().isPlace())
					return;
				//recherche l'ElementView
				var oElementView = mapPlanNodes.get(oPlanCharge.get().getId()).stream()
						.filter(ev -> ev.getElement().getId().equals(elem.getId())).findFirst();

				if (event.getCode() == KeyCode.R) {
					oElementView.get().rotate90();
					event.consume();
				} else if (event.getCode() == KeyCode.DELETE) {
					// supprime l'elementView du pane
					pane.getChildren().remove(oElementView.get());
					// supprime l'elementView de la liste des ElementViews du plan
					mapPlanNodes.get(oPlanCharge.get().getId()).remove(oElementView.get());
					//indique que l'élément n'est plus placé
					oElementView.get().getElement().getLocalisation().setPlace(false);
					// rafraîchit la liste pour retirer la mise en évidence de l'élément
					lstElements.refresh();
					event.consume();
				}
			});
			
			//désélectionne les ElementView de l'ancien Plan si on change de plan
			cbPlans.valueProperty().addListener((_, oldV, _) -> {
				if (oldV != null) {
					mapPlanNodes.get(oldV.getId()).forEach(ev -> ev.setSelected(false));

				}
			});

			// s'abonne aux évènements du modèle pour réagir aux changements
			// (association d'un élément à une pièce, ajout d'une pièce à un plan,...)
			facade.addObserver(this);

		} catch (InstallationException e) {
			ctrl.showErreur(I18N.getString("err.noInstall"));
		}
	}

	/**
	 * Permet de déplacer un élementView
	 * 
	 * @param elementView
	 */
	private void moveBehaviour(ElementView elementView) {
		// mémorise la différence entre la position de la souris sur la scene et
		// la translation de l'élement sur le pane
		final Delta delta = new Delta();

		elementView.setOnMousePressed(e -> {
			// décalage entre la position de la souris sur la scene et la
			// translation dans le Pane 
			delta.x = e.getSceneX() - elementView.getTranslateX();
			delta.y = e.getSceneY() - elementView.getTranslateY();
			elementView.toFront();

			// sélectionne l'élément dans la liste et indirectement selectionne son ElementView dans le pane 
			//==> le déplacement graphique sélectionne l'élément ds la liste
			lstElements.getSelectionModel().select(elementView.getElement());
			e.consume();
		});

		elementView.setOnMouseDragged(e -> {
			// Calcule la nouvelle translation
			double x = e.getSceneX() - delta.x;
			double y = e.getSceneY() - delta.y;

			elementView.setTranslateX(x);
			elementView.setTranslateY(y);

			e.consume();
		});

		elementView.setOnMouseReleased(e -> {

			Element elt = elementView.getElement();

			elt.getLocalisation().setX(elementView.getTranslateX());
			elt.getLocalisation().setY(elementView.getTranslateY());

			e.consume();
		});
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		//Défini le look des cellules de la liste d'éléments et le drag and drop vers le Canvas
		lstElements.setCellFactory(_ -> {
			ListCell<Element> cell = new ListCell<>() {
				@Override
				protected void updateItem(Element element, boolean empty) {
					super.updateItem(element, empty);

					if (empty || element == null) {
						setText(null);
						setStyle(null);
					} else {
						setText(element.getCode() + ":" + element.getAppareil().getNom());
						// met en évidence les éléments déjà placés sur le plan
						if (element.getLocalisation() != null && element.getLocalisation().isPlace())
							setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
						else
							setStyle(null);
					}
				}
			};

			// On Drag
			cell.setOnDragDetected(ev -> {

				Element element = cell.getItem();
				// si l'élément est déjà placé on quitte
				if (element == null || element.getLocalisation().isPlace())
					return;
				//On mémorise l'id de l'élément qu'on veut positionner placer
				Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
				ClipboardContent cc = new ClipboardContent();
				cc.putString(element.getId().toString());// garde l'identifiant de l'élément
				db.setContent(cc);

				ev.consume();
			});

			return cell;
		});
		// Drag Over sur le Pane
		pane.setOnDragOver(ev -> {

			if (ev.getGestureSource() != pane && ev.getDragboard().hasString()) {

				ev.acceptTransferModes(TransferMode.COPY);
			}

			ev.consume();
		});
		// Drop
		pane.setOnDragDropped(ev -> {

			Dragboard db = ev.getDragboard();

			if (!db.hasString())
				return;
			// recherche l'élément que l'on pose sur le dessin à partir de son id dans le DragBoard
			var oElement = lstElements.getItems().stream()
					.filter(elem -> elem.getId().equals(Integer.parseInt(db.getString()))).findFirst();
			if (oElement.isPresent()) {
				// spécifie ses nouvelles coordonnées et crée son ElementView
				Element element = oElement.get();
				element.getLocalisation().setX(ev.getX());
				element.getLocalisation().setY(ev.getY());
				ElementView elementView = creeElementView(element);
				pane.getChildren().add(elementView);// rajoute l'elementView au pane actif
				// ajoute l'elementView aux elements du plan
				mapPlanNodes.get(oPlanCharge.get().getId()).add(elementView);
				element.getLocalisation().setPlace(true);//indique qu'il est placé

				elementView.setSelected(true);//sélectionne l'élément que l'on vient de mettre
				lstElements.refresh();// met à jour la mise en évidence dans la liste
			}

			ev.setDropCompleted(true);
			ev.consume();
		});

		lstElements.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// Pour l'élément selectionné, s'il est placé, on active la boundingBox de son ElementView associé
		lstElements.getSelectionModel().selectedItemProperty().addListener((_, oldElem, newElem) -> {
			if (oldElem != null && oPlanCharge.isPresent()) {
				// recherche son ElementView s'il existe et le désélectionne
				mapPlanNodes.get(oPlanCharge.get().getId()).stream()
						.filter(ev -> ev.getElement().getId().equals(oldElem.getId())).findFirst()
						.ifPresent(ev -> ev.setSelected(false));
			}

			if (newElem != null && oPlanCharge.isPresent()) {
				// recherche son ElementView s'il existe et le sélectionne
				mapPlanNodes.get(oPlanCharge.get().getId()).stream()
						.filter(ev -> ev.getElement().getId().equals(newElem.getId())).findFirst()
						.ifPresent(ev -> ev.setSelected(true));
			}
		});

		// permet de déselectionné l'élément lorsque l'on clique sur le canvas dans une zone sans ElementView
		pane.setOnMouseClicked(ev -> {
			if (ev.getTarget() instanceof Canvas)
				lstElements.getSelectionModel().clearSelection();
		});

		// Affichage de la ComboBox des plans: on n'affiche que le nom du plan
		// (ex: "rez", "etage1") plutôt que toute la représentation de l'objet
		cbPlans.setConverter(new StringConverter<Plan>() {
			@Override
			public String toString(Plan plan) {
				return plan == null ? "" : plan.getNom();
			}

			@Override
			public Plan fromString(String str) {
				return null;
			}
		});

		// Affichage de la ComboBox de filtrage par pièce
		// (la valeur null correspond à "toutes les pièces")
		cbPieces.setConverter(new StringConverter<Piece>() {
			@Override
			public String toString(Piece piece) {
				return piece == null ? I18N.getString("piece.toutes") : piece.getNom();
			}

			@Override
			public Piece fromString(String str) {
				return null;
			}
		});
		// applique le filtre lorsqu'on change la pièce sélectionnée
		cbPieces.valueProperty().addListener((_, _, piece) -> appliquerFiltrePiece(piece));

	}

	/**
	 * Crée un ElementView à partir d'un élément et le positionne sur le pane s'il est déjà placé 
	 * On lui associe aussi les évènements 
	 * 		de Rotation via les touches CTRL+R
	 * 		de suppression de l'ElementView via Delete (on ne supprime pas l'élément!) 
	 * 		de déplacement sur le pane
	 * 
	 * @param element
	 * @return
	 */
	private ElementView creeElementView(Element element) {
		assert element != null && element.getLocalisation() != null
				: "L'élément ne peut pas être à null et doit avoir une localisation";
		ElementView elementView = new ElementView(element);
		elementView.setTranslateX(element.getLocalisation().getX());
		elementView.setTranslateY(element.getLocalisation().getY());
		moveBehaviour(elementView);
		return elementView;
	}

	/**
	 * Recalcule la liste observable des éléments de chaque plan à partir de la
	 * façade. Comme les listes du plan courant sont reliées à la liste filtrée
	 * (et donc à la ListView), celles-ci se rafraîchissent automatiquement. Utile
	 * lorsqu'un élément vient d'être associé à une pièce (et donc à un plan).
	 */
	private void rafraichirListesElements() {
		for (Plan plan : cbPlans.getItems()) {
			ObservableList<Element> obs = mapPlanElements.get(plan.getId());
			if (obs != null)
				obs.setAll(facade.getElementsPlan(plan));
		}
		lstElements.refresh();
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
		// Le rafraîchissement graphique doit se faire sur le thread JavaFX
		Platform.runLater(() -> {
			// Un élément a été (ré)associé à une pièce: on recalcule les listes
			// d'éléments des plans pour que l'élément apparaisse dans le bon plan.
			if (item.hasEventFromClasse(Classe.LOCALISATION))
				rafraichirListesElements();

			// Une pièce a été ajoutée/supprimée (éventuellement sur le plan courant):
			// on met à jour la ComboBox de filtrage et les listes d'éléments.
			if (item.hasEventFromClasse(Classe.PIECE)) {
				item.getEventFromClasse(Classe.PIECE).ifPresent(ev -> {
					if (!(ev.element() instanceof Piece piece) || oPlanCharge.isEmpty())
						return;
					boolean memePlan = piece.getPlan() != null
							&& piece.getPlan().getId().equals(oPlanCharge.get().getId());
					if (memePlan) {
						if (ev.op() == TypeOperation.INSERT) {
							if (!cbPieces.getItems().contains(piece))
								cbPieces.getItems().add(piece);
						} else if (ev.op() == TypeOperation.DELETE) {
							cbPieces.getItems().remove(piece);
						}
					}
				});
				rafraichirListesElements();
			}
		});
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
		log.info("Plus d'évènements à recevoir pour la vue d'implantation");
	}

}

//Utilisé pour le déplacement d'un ElementView
class Delta {
	double x;
	double y;
};
