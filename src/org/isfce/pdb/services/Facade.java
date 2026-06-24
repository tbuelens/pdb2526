package org.isfce.pdb.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Flow.Subscriber;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.isfce.pdb.dao.DAOFactory;
import org.isfce.pdb.exceptions.InstallationException;
import org.isfce.pdb.model.Element;
import org.isfce.pdb.model.Installation;
import org.isfce.pdb.model.Localisation;
import org.isfce.pdb.model.Piece;
import org.isfce.pdb.model.Plan;
import org.isfce.pdb.model.TypePiece;
import org.isfce.pdb.services.ListMessages.Classe;
import org.isfce.pdb.services.ListMessages.Evenement;
import org.isfce.pdb.view.bundle.I18N;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Facade {
	private DAOFactory factory;
	private Installation installation;
	// Le publisher
	private PublisherEvent publisher;

	// liste des plans de l'installation chargée
	private List<Plan> plans = new ArrayList<Plan>();
	// pièces de l'installation chargée
	private List<Piece> pieces = new ArrayList<Piece>();
	// éléments de l'installation chargée
	private List<Element> elements = new ArrayList<Element>();

	// propriétés du projet
	@Getter
	private Properties properties = new Properties();

	public Facade(DAOFactory factory) {
		this.factory = factory;
		// Le Publisher pour publier les évènements
		publisher = new PublisherEvent();
		// charges le fichier de propriétés
		chargerProperties();
	}

	/**
	 * Charge l'installation correspondant à l'ID passé en paramètre à partir de la
	 * base de données « pdb2526 ». L'installation, ses plans, ses pièces et ses
	 * éléments (avec leur localisation éventuelle) sont mémorisés par la façade.
	 *
	 * @param id l'identifiant de l'installation à charger
	 * @throws InstallationException si aucune installation ne correspond à cet ID
	 */
	public void chargeInstallation(int id) throws InstallationException {

		// récupère l'installation depuis la BD (exception si elle n'existe pas)
		installation = factory.getInstallationDao().getFromID(id)
				.orElseThrow(() -> new InstallationException(I18N.getString("err.install.inconnue")));

		// charge les plans de l'installation
		plans = factory.getPlanDao().getListePlanFromInstallation(id);

		// charge les pièces de l'installation; la liste ne renseigne pas le plan
		// associé, on recharge donc chaque pièce complète pour récupérer son plan
		List<Piece> piecesInstallation = factory.getPieceDAO().getListeFromInstallation(id);
		pieces = new ArrayList<>(piecesInstallation.size());
		for (Piece piece : piecesInstallation) {
			// getFromID renseigne le plan explicite (FKPLAN_PIE) et le type de la pièce
			Piece complete = factory.getPieceDAO().getFromID(piece.getId()).orElse(piece);
			// si la pièce n'a pas de plan explicite, on l'associe au plan de son étage
			autoAssocierPlanParEtage(complete);
			pieces.add(complete);
		}

		// charge les éléments de l'installation et leur localisation éventuelle
		// (un élément placé sur un plan possède une localisation pointant vers sa pièce)
		elements = factory.getElementDao().getListeFromInstallation(id);
		var localisationDao = factory.getLocalisationDao();
		for (Element element : elements) {
			localisationDao.getFromID(element.getId()).ifPresent(loc -> {
				// réutilise l'instance de pièce déjà chargée (et donc associée à son plan)
				if (loc.getPiece() != null)
					pieces.stream().filter(pc -> pc.getId().equals(loc.getPiece().getId()))
							.findFirst().ifPresent(loc::setPiece);
				element.setLocalisation(loc);
			});
		}
	}

	/**
	 * Retourne la liste de toutes les installations, triées de la plus récente à
	 * la plus ancienne (voir {@link org.isfce.pdb.dao.IInstallationDao}). Permet à
	 * la vue de chargement de proposer une installation à sélectionner.
	 *
	 * @return la liste des installations
	 */
	public List<Installation> getListeInstallations() {
		return factory.getInstallationDao().getListe(null);
	}

	/**
	 * Obtenir l'installation courante
	 *
	 * @return
	 * @throws InstallationException
	 */
	public Installation getCurrentInstallation() throws InstallationException {
		if (installation == null)
			throw new InstallationException(I18N.getString("err.noInstall"));
		return installation;
	}

	/**
	 * retourne tous les types de pièces
	 * 
	 * @return
	 */
	public List<TypePiece> getTypePiece() {
		return factory.getTypePieceDAO().getListe(null);
	}

	/**
	 * Ajout d'une nouvelle pièce
	 */
	public void insertPiece(Piece piece) throws InstallationException {
		try {
			factory.getPieceDAO().insert(piece);
			// associe la nouvelle pièce au plan de son étage (FKPLAN_PIE non encore
			// renseigné) pour qu'elle apparaisse dans la vue d'implantation du bon plan
			autoAssocierPlanParEtage(piece);
			// mémorise la pièce pour que getPiecesPlan/getElementsPlan la prennent en
			// compte (notamment lors d'un changement/rechargement de plan)
			pieces.add(piece);
			// publie l'évènement
			Map<Classe, Evenement> messages = new HashMap<>();
			messages.put(Classe.PIECE, new Evenement(TypeOperation.INSERT, piece));
			publisher.submit(new ListMessages(messages));
		} catch (Exception e) {
			if (e instanceof InstallationException exception)
				throw exception;
			// toute autre exception ne doit pas être avalée silencieusement
			log.error("Erreur lors de l'insertion de la pièce: " + e.getMessage());
		}

	}

	public List<Piece> getListePieces() {
		if (installation != null) {
			List<Piece> liste = factory.getPieceDAO().getListeFromInstallation(installation.getId());
			// associe chaque pièce au plan de son étage (si pas de plan explicite) afin
			// qu'une association élément->pièce conserve le lien vers le plan
			liste.forEach(this::autoAssocierPlanParEtage);
			return liste;
		} else
			return List.of();
	}

	/**
	 * Associe, en mémoire, une pièce au plan de son étage lorsqu'elle n'a pas
	 * encore de plan explicite (FKPLAN_PIE non renseigné en base). L'étage
	 * représenté par un plan est déduit de son nom (voir {@link #etageDuPlan(Plan)}).
	 * Sans cette association, la pièce (et ses éléments) n'apparaîtrait jamais dans
	 * la vue d'implantation, qui filtre tout par le plan de la pièce.
	 *
	 * @param piece la pièce à associer (ignorée si null ou déjà liée à un plan)
	 */
	private void autoAssocierPlanParEtage(Piece piece) {
		if (piece == null || piece.getPlan() != null || piece.getEtage() == null)
			return;
		int etagePiece = piece.getEtage().intValue();
		plans.stream().filter(pl -> etageDuPlan(pl) == etagePiece).findFirst().ifPresent(piece::setPlan);
	}

	/**
	 * Déduit l'étage représenté par un plan à partir de son nom: "rez"/"rdc"
	 * correspond au rez-de-chaussée (étage 0), sinon on prend le nombre éventuel
	 * présent dans le nom (ex: "etage1" -> 1, "etage-1" -> -1).
	 *
	 * @param plan le plan
	 * @return l'étage correspondant, ou {@link Integer#MIN_VALUE} si indéterminable
	 */
	private static int etageDuPlan(Plan plan) {
		String nom = plan.getNom() == null ? "" : plan.getNom().trim().toLowerCase();
		if (nom.startsWith("rez") || nom.startsWith("rdc"))
			return 0;
		Matcher m = Pattern.compile("-?\\d+").matcher(nom);
		return m.find() ? Integer.parseInt(m.group()) : Integer.MIN_VALUE;
	}

	/**
	 * Suppression d'une pièce
	 * 
	 * @param piece
	 * @return
	 * @throws InstallationException
	 */
	public boolean deletePiece(Piece piece) throws InstallationException {
		// tente de supprimer la pièce mais elle ne doit pas encore être utilisée
		// (Localisation)
		// ici je fais juste une suppression et vérifie si je n'ai pas une exception
		boolean ok = false;
		try {
			ok = factory.getPieceDAO().delete(piece);
			// retire la pièce de la liste mémorisée pour rester cohérent avec getPiecesPlan
			pieces.removeIf(p -> p.getId() != null && p.getId().equals(piece.getId()));
			// publie l'évènement
			Map<Classe, Evenement> messages = new HashMap<>();
			messages.put(Classe.PIECE, new Evenement(TypeOperation.DELETE, piece));
			publisher.submit(new ListMessages(messages));
		} catch (Exception e) {
			if (e instanceof InstallationException exc)
				throw exc;
		}
		return ok;
	}

	/**
	 * Mise à jour d'une pièce
	 * 
	 * @param piece
	 * @return
	 * @throws InstallationException
	 */
	public boolean updatePiece(Piece piece) throws InstallationException {
		boolean ok = false;
		try {
			ok = factory.getPieceDAO().update(piece);
			// publie l'évènement
			Map<Classe, Evenement> messages = new HashMap<>();
			messages.put(Classe.PIECE, new Evenement(TypeOperation.UPDATE, piece));
			publisher.submit(new ListMessages(messages));
		} catch (Exception e) {
			if (e instanceof InstallationException exc)
				throw exc;
		}
		return ok;
	}

	/**
	 * charge une pièce à partir de son id
	 * 
	 * @param id
	 * @return
	 */
	public Optional<Piece> getPiece(Integer id) {
		return factory.getPieceDAO().getFromID(id);
	}

	/**
	 * Retourne la liste des plans de l'installation
	 * 
	 * @return
	 * @throws InstallationException
	 */
	public List<Plan> getListePlans() {
		return plans;
	}

	/**
	 * Ajoute un nouveau plan à l'installation courante. Le plan est rendu
	 * persistant, mémorisé dans la liste des plans de l'installation chargée puis
	 * un évènement de type PLAN (INSERT) est publié pour permettre aux vues de se
	 * rafraîchir.
	 *
	 * @param plan le plan à ajouter
	 * @throws InstallationException en cas de problème de persistance
	 */
	public void insertPlan(Plan plan) throws InstallationException {
		try {
			factory.getPlanDao().insert(plan);
			// mémorise le plan uniquement s'il appartient à l'installation chargée
			// (le numéro saisi dans la vue peut viser une autre installation)
			if (installation != null && plan.getInstallation() != null
					&& installation.getId().equals(plan.getInstallation().getId()))
				plans.add(plan);
			// publie l'évènement
			Map<Classe, Evenement> messages = new HashMap<>();
			messages.put(Classe.PLAN, new Evenement(TypeOperation.INSERT, plan));
			publisher.submit(new ListMessages(messages));
		} catch (Exception e) {
			if (e instanceof InstallationException exc)
				throw exc;
			log.error("Erreur lors de l'insertion du plan: " + e.getMessage());
			throw new InstallationException(I18N.getString("err.insertion.impossible"));
		}
	}

	/**
	 * Retourne les pièces associées au plan
	 * 
	 * @param plan
	 * @return
	 */
	public List<Piece> getPiecesPlan(Plan plan) {
		return pieces.stream().filter(p -> plan.equals(p.getPlan())).toList();
	}

	/**
	 * Retourne la liste de tous les éléments de l'installation chargée
	 *
	 * @return la liste des éléments
	 */
	public List<Element> getListeElements() {
		return elements;
	}

	/**
	 * Associe un élément à une pièce. Si l'élément ne possède pas encore de
	 * localisation, une nouvelle localisation (non placée) est créée, sinon on
	 * remplace simplement sa pièce. Un évènement de type LOCALISATION est publié
	 * pour permettre aux vues de se rafraîchir.
	 *
	 * @param element l'élément à associer
	 * @param piece   la pièce dans laquelle se trouve l'élément
	 */
	public void associeElementPiece(Element element, Piece piece) {
		Localisation loc = element.getLocalisation();
		if (loc == null) {
			// nouvelle localisation, l'élément n'est pas encore placé sur le plan
			loc = Localisation.builder().elementId(element.getId()).piece(piece).x(0.0).y(0.0).angle(0.0).place(false)
					.build();
			element.setLocalisation(loc);
		} else {
			// on change uniquement la pièce associée
			loc.setPiece(piece);
		}
		// publie l'évènement pour avertir les vues abonnées
		Map<Classe, Evenement> messages = new HashMap<>();
		messages.put(Classe.LOCALISATION, new Evenement(TypeOperation.UPDATE, element));
		publisher.submit(new ListMessages(messages));
	}

	/**
	 * retourne les éléments d'une pièce
	 *
	 * @param piece
	 * @return
	 */
	public List<Element> getElementPiece(Piece piece) {
		return elements.stream()
				.filter(e -> piece.equals(e.getLocalisation() == null ? null : e.getLocalisation().getPiece()))
				.toList();
	}

	/***
	 * Retourne la liste des éléments déjà placés sur le plan
	 * 
	 * @param plan
	 * @return
	 */
	public List<Element> getElementsPlacesDansPlanBOL(Plan plan) {
		assert plan != null : " Le plan est à null";
		// recherche les élements placés sur le plan p
		return elements.stream()
				.filter(element -> (element.getLocalisation() != null && element.getLocalisation().isPlace()
						&& plan.getId() == (element.getLocalisation().getPiece().getPlan() != null
								? element.getLocalisation().getPiece().getPlan().getId()
								: null)))
				.toList();
	}

	/**
	 * Retourne tous les éléments qui sont ou doivent apparaître sur le plan
	 * 
	 * @param plan
	 */
	public List<Element> getElementsPlan(Plan plan) {
		assert plan != null : " Le plan est à null";
		return elements.stream()
				.filter(element -> (element.getLocalisation() != null
						&& plan.getId() == (element.getLocalisation().getPiece().getPlan() != null
								? element.getLocalisation().getPiece().getPlan().getId()
								: null)))
				.toList();
	}

	/**
	 * Sauvegarde les localisations des éléments de l'installation chargée. Pour
	 * chaque élément possédant une localisation:
	 * <ul>
	 * <li>s'il est placé sur le plan, on met à jour sa localisation, et si elle
	 * n'existe pas encore en base, on l'insère;</li>
	 * <li>s'il n'est plus placé, on supprime sa localisation de la base (sans
	 * effet si elle n'existait pas).</li>
	 * </ul>
	 * Un évènement de type LOCALISATION (CHARGEMENT) est publié une fois la
	 * sauvegarde terminée.
	 *
	 * @throws InstallationException en cas de problème de persistance
	 */
	public void sauvegarderLocalisations() throws InstallationException {
		try {
			var dao = factory.getLocalisationDao();
			for (Element element : elements) {
				Localisation loc = element.getLocalisation();
				if (loc == null)
					continue;
				if (loc.isPlace()) {
					// met à jour la localisation, sinon l'insère si elle n'existe pas encore
					if (!dao.update(loc))
						dao.insert(loc);
				} else {
					// l'élément n'est plus placé: on retire sa localisation éventuelle
					dao.delete(loc);
				}
			}
			// avertit les vues que les localisations ont été sauvées
			Map<Classe, Evenement> messages = new HashMap<>();
			messages.put(Classe.LOCALISATION, new Evenement(TypeOperation.CHARGEMENT, installation));
			publisher.submit(new ListMessages(messages));
		} catch (Exception e) {
			if (e instanceof InstallationException exc)
				throw exc;
			log.error("Erreur lors de la sauvegarde des localisations: " + e.getMessage());
			throw new InstallationException(I18N.getString("err.save"));
		}
	}

	/**
	 * Permet de s'abonner aux évènements sur le modèle
	 */

	public void addObserver(Subscriber<ListMessages> obs) {
		publisher.addObserver(obs);
	}

	private void chargerProperties() {
		String fichier = "./ressources/installation.properties";
		// charge les propriétes de l'application
		try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
			properties.load(br);// charge toutes les propriétés du fichier dans la map
		} catch (IOException e) {
			log.error("Problème de chargement du fichier " + fichier + " : " + e.getMessage());
		}
	}

}
