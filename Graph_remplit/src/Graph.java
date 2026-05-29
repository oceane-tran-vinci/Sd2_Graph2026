import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * ============================
 * GRAPH (Graphe orienté pondéré)
 * ============================
 *
 * Ce projet modélise un graphe :
 * - Les sommets = Localisation (nodes)
 * - Les arcs = Arc (edges)
 *
 * Structure principale :
 * - localisationsMap : accès rapide par ID
 * - listeAdjacence : représentation du graphe (liste d'adjacence)
 *
 * Tous les algorithmes (BFS / Dijkstra) reposent dessus.
 */
public class Graph {

  private final String localisationsFile;
  private final String roadsFile;
  private final Map<Long, Localisation> localisationsMap = new HashMap<>(); // Map ID -> Node (accès O(1))
  private final Map<Localisation, ArrayList<Arc>> listeAdjacence = new HashMap<>(); // Liste d’adjacence : Node -> arcs sortants

  /**
   * CONSTRUCTEUR
   * Initialise le graphe en chargeant les fichiers CSV.
   *
   * Étapes :
   * 1. Charger les sommets (Localisation)
   * 2. Charger les arcs (routes)
   * 3. Construire la liste d’adjacence
   */
  public Graph(String localisations, String roads) {
    this.localisationsFile = localisations;
    this.roadsFile = roads;

    // Appeler les 2 méthodes pour charger les fichiers
    loadLocalisations();
    loadArcs();
  }


  /**
   * Détermine l'ensemble des zones inondées par propagation depuis un ou plusieurs points de départ.
   * Utilise l'algorithme de parcours en largeur (BFS - Breadth-First Search).
   *
   * @param idsOrigin Les identifiants des localisations de départ (les sources de l'inondation).
   * @param epsilon La marge de tolérance d'altitude pour que l'eau puisse couler (ex: l'eau peut monter de epsilon mètres).
   * @return Un tableau contenant toutes les localisations inondées, dans l'ordre où elles ont été touchées par l'eau.
   */
  public Localisation[] determinerZoneInondee(long[] idsOrigin, double epsilon) {

    // 1. GESTION DES CAS AUX LIMITES (Edge cases)
    // Si la liste des sources est vide ou nulle, il n'y a pas d'inondation. On retourne un tableau vide.
    if (idsOrigin == null || idsOrigin.length == 0) {
      return new Localisation[0];
    }

    // 2. INITIALISATION DES STRUCTURES DE DONNÉES (Le cœur de l'algorithme BFS)

    // La "Queue" (File) dicte le prochain élément à explorer (méthode FIFO : Premier entré, premier sorti).
    // On utilise ArrayDeque car c'est l'implémentation la plus rapide en Java pour une file (O(1) pour add/poll).
    Queue<Localisation> queue = new ArrayDeque<>();

    // Le "Set" (Ensemble) agit comme notre registre historique.
    // Il empêche de visiter la même zone deux fois et évite les boucles infinies.
    // HashSet garantit une vérification instantanée en O(1).
    Set<Localisation> flooded = new HashSet<>();

    // La "List" conserve l'ordre chronologique exact de l'inondation, ce que le HashSet ne peut pas faire.
    List<Localisation> visitedOrder = new ArrayList<>();

    // 3. INITIALISATION DU PARCOURS (Préparation des sources)
    for (long id : idsOrigin) {
      // On récupère l'objet Localisation depuis son ID (via une map globale supposée exister dans la classe)
      Localisation source = localisationsMap.get(id);

      // Sécurité 1 : On vérifie que l'ID correspond bien à une zone existante (source != null).
      // Sécurité 2 : flooded.add(source) ajoute la source ET renvoie "true" si elle n'y était pas déjà.
      // Cela évite de traiter deux fois la même source si le tableau idsOrigin contient des doublons.
      if (source != null && flooded.add(source)) {
        queue.add(source);        // On la met dans la file d'attente pour explorer ses voisins plus tard
        visitedOrder.add(source); // On l'enregistre dans notre résultat final chronologique
      }
    }

    // 4. BOUCLE PRINCIPALE DU PARCOURS EN LARGEUR (La propagation de l'eau)
    // Tant qu'il reste des zones inondées dont on n'a pas encore vérifié les voisins...
    while (!queue.isEmpty()) {

      // poll() retire et renvoie le premier élément de la file (la plus ancienne zone inondée non traitée)
      Localisation currentLoc = queue.poll();

      // On récupère tous les chemins (arcs) qui partent de cette zone
      List<Arc> arcsSortants = getArcsSortants(currentLoc);

      // On teste chaque voisin pour voir si l'eau peut s'y propager
      for (Arc arc : arcsSortants) {
        Localisation voisin = arc.getTarget();

        // LA RÈGLE MÉTIER ET LA SÉCURITÉ ANTI-BOUCLE SONT COMBINÉES ICI :
        // Condition A : L'altitude du voisin est-elle atteignable ? (L'eau descend, ou monte au maximum de epsilon)
        // Condition B : Ce voisin est-il nouveau ? (flooded.add() renverra false si la zone est déjà inondée)
        // Le && est un court-circuit : si l'altitude est mauvaise, Java ne tente même pas de l'ajouter au Set.
        if (voisin.getAltitude() <= currentLoc.getAltitude() + epsilon && flooded.add(voisin)) {

          // Si l'eau l'atteint et qu'il est nouveau, on l'ajoute à la file pour qu'il inonde ses propres voisins au prochain tour
          queue.add(voisin);
          // On l'ajoute à l'historique chronologique
          visitedOrder.add(voisin);
        }
      }
    }

    // 5. RETOUR DU RÉSULTAT
    // On convertit notre ArrayList dynamique en un tableau statique de la bonne taille.
    // L'argument "new Localisation[0]" indique juste à Java le type de tableau à créer.
    return visitedOrder.toArray(new Localisation[0]);
  }


  /**
   * Trouve le chemin le plus court (en nombre d'arcs) pour aller d'un point A à un point B
   * tout en évitant les zones inondées.
   * * ALGORITHME UTILISÉ : Parcours en Largeur (BFS).
   * Pourquoi ? Parce que dans un graphe non pondéré (où chaque route vaut "1 étape"),
   * le BFS est mathématiquement garanti de trouver le chemin le plus court en premier.
   */
  public Deque<Localisation> trouverCheminLePlusCourtPourContournerLaZoneInondee(long idOrigin,
      long idDestination, Localisation[] floodedZone) {

    // 1. VÉRIFICATION DES ENTRÉES
    Localisation origin = localisationsMap.get(idOrigin);
    Localisation destination = localisationsMap.get(idDestination);

    if (origin == null || destination == null) {
      throw new RuntimeException("Localisation introuvable.");
    }

    // Cas trivial : on est déjà sur place.
    if (origin.equals(destination)) {
      Deque<Localisation> path = new ArrayDeque<>();
      path.add(origin);
      return path;
    }

    // 2. OPTIMISATION DE COMPLEXITÉ (Très important pour l'examen)
    // On convertit le tableau en HashSet.
    // Recherche dans un tableau = O(N). Recherche dans un HashSet = O(1).
    // Comme on va vérifier cette condition pour CHAQUE voisin visité, c'est vital.
    Set<Localisation> floodedZoneSet = new HashSet<>(Arrays.asList(floodedZone));

    // Sécurité : Impossible de partir ou d'arriver sous l'eau.
    if (floodedZoneSet.contains(origin)) {
      throw new RuntimeException("Le point d'origine " + idOrigin + " est inondé !");
    }
    if (floodedZoneSet.contains(destination)) {
      throw new RuntimeException("Le point de destination " + idDestination + " est inondé !");
    }

    // 3. INITIALISATION DU BFS
    // La File dicte l'ordre d'exploration (FIFO : le premier découvert est le premier exploré).
    Queue<Localisation> queue = new ArrayDeque<>();

    // Le Set évite de tourner en rond et de repasser par un nœud déjà traité en temps constant O(1).
    Set<Localisation> visited = new HashSet<>();

    // La Map "Parent" agit comme le fil d'Ariane.
    // Clé = Le nœud actuel | Valeur = Le nœud d'où l'on vient.
    Map<Localisation, Localisation> parent = new HashMap<>();

    // Setup du point de départ
    queue.add(origin);
    visited.add(origin);
    parent.put(origin, null); // L'origine n'a pas de parent

    boolean found = false; // Drapeau pour l'arrêt prématuré (Early Exit)

    // 4. BOUCLE D'EXPLORATION
    // On boucle tant qu'il y a des chemins à explorer ET qu'on n'a pas trouvé la cible
    while (!queue.isEmpty() && !found) {

      Localisation current = queue.poll(); // On sort le nœud le plus ancien de la file
      List<Arc> arcsSortants = getArcsSortants(current);

      for (Arc arc : arcsSortants) {
        Localisation voisins = arc.getTarget();

        // RÈGLE MÉTIER : On ignore les culs-de-sac (déjà visités) et les zones inondées.
        if (visited.contains(voisins) || floodedZoneSet.contains(voisins)) {
          continue; // Passe directement au voisin suivant
        }

        // Si le chemin est valide, on l'enregistre
        queue.add(voisins);
        visited.add(voisins);

        // On mémorise que "voisins" a été découvert grâce à "current"
        parent.put(voisins, current);

        // OPTIMISATION BFS : Arrêt immédiat
        // Dès qu'on touche la destination, on arrête tout. Pas besoin d'explorer le reste du graphe !
        if (voisins.equals(destination)) {
          found = true;
          break; // Casse la boucle for
        }
      }
    }

    // 5. RECONSTRUCTION DU RÉSULTAT
    if (!found) {
      // La file s'est vidée sans jamais trouver la cible : elle est encerclée par l'eau.
      throw new RuntimeException("Pas de chemin de " + idOrigin + " à " + idDestination
          + " évitant la zone inondée.");
    }

    // On utilise notre fil d'Ariane pour remonter de la destination jusqu'à l'origine
    Deque<Localisation> path = reconstructPath(destination, parent);
    return path;
  }


  /**
   * Détermine la chronologie exacte de l'inondation (à quelle heure chaque lieu sera sous l'eau).
   * ALGORITHME UTILISÉ : Algorithme de Dijkstra.
   * Pourquoi Dijkstra ? Parce que l'eau ne se propage pas à vitesse constante (elle accélère en descente).
   * Le "coût" d'un chemin n'est donc pas juste le nombre de routes (BFS), mais le TEMPS physique écoulé.
   * * @param idsOrigin Les points de départ de l'inondation.
   * @param vWaterInit La vitesse initiale de l'eau.
   * @param k Le coefficient d'influence de la pente sur la vitesse de l'eau.
   * @return Une Map triée contenant les lieux inondés et le temps exact de leur inondation.
   */
  public Map<Localisation, Double> determinerChronologieDeLaCrue(long[] idsOrigin,
      double vWaterInit, double k) {

    // 1. VÉRIFICATION DES ENTRÉES (Sécurité)
    if (idsOrigin == null || idsOrigin.length == 0) {
      throw new IllegalArgumentException("Aucune source trouvée.");
    }
    if (vWaterInit < 0) {
      throw new IllegalArgumentException("La vitesse de l'eau ne peut être négative");
    }

    // 2. INITIALISATION DES STRUCTURES (Le moteur de Dijkstra)
    // tFlood agit comme les "étiquettes de distance" dans le Dijkstra classique.
    // vWater permet de garder en mémoire la vitesse de l'eau lorsqu'elle atteint un lieu spécifique.
    Map<Localisation, Double> tFlood = new HashMap<>();
    Map<Localisation, Double> vWater = new HashMap<>();

    // ÉTAPE CLÉ DE DIJKSTRA : On initialise tous les temps à l'infini (Double.MAX_VALUE).
    // Cela signifie qu'au début, on considère que l'eau n'atteindra jamais aucun lieu.
    localisationsMap.values().forEach((l) -> tFlood.put(l, Double.MAX_VALUE));

    // La File à Priorité (PriorityQueue) est le cœur de Dijkstra.
    // Elle garantit qu'on traitera TOUJOURS le lieu inondé le plus tôt (tFlood le plus petit) en premier.
    PriorityQueue<Localisation> priorityQueue = new PriorityQueue<>(
        Comparator.comparingDouble(tFlood::get));

    // 3. PRÉPARATION DES POINTS DE DÉPART (Les sources de l'inondation)
    for (long id : idsOrigin) {
      Localisation source = localisationsMap.get(id);
      if (source != null) {
        vWater.put(source, vWaterInit);
        tFlood.put(source, 0.0); // Le temps d'inondation d'une source est T=0.
        priorityQueue.add(source); // On les place dans la file d'attente
      }
    }

    // 4. BOUCLE PRINCIPALE DE DIJKSTRA
    while (!priorityQueue.isEmpty()) {

      // poll() sortira toujours le lieu qui a la valeur tFlood la plus basse.
      // C'est vital pour garantir qu'on explore l'évolution de la crue de manière chronologique.
      Localisation currentLoc = priorityQueue.poll();

      double currentTime = tFlood.get(currentLoc);
      double currentVitesse = vWater.get(currentLoc);

      List<Arc> arcsSortants = getArcsSortants(currentLoc);

      // 5. EXPLORATION DES VOISINS (Relaxation des arêtes)
      for (Arc arcsSortant : arcsSortants) {
        Localisation voisin = arcsSortant.getTarget();

        // CALCUL MÉTÉOROLOGIQUE (Le twist de ce problème)
        double pente = arcsSortant.getPente();

        // Nouvelle vitesse de l'eau sur cette route précise
        double vWaterVoisin = currentVitesse + (k * pente);

        // Si la pente monte tellement que la vitesse devient <= 0, l'eau s'arrête.
        // On ignore donc cette route et on passe à la suivante.
        if (vWaterVoisin <= 0) {
          continue;
        }

        // Temps nécessaire à l'eau pour parcourir cette route (Temps = Distance / Vitesse)
        double tArc = arcsSortant.getDist() / vWaterVoisin;

        // Temps total écoulé depuis le début de la crue pour que l'eau arrive au bout de cette route
        double newT = currentTime + tArc;

        // CONDITION DE MISE À JOUR DE DIJKSTRA
        // Si ce nouveau temps calculé est plus rapide que le temps d'inondation qu'on connaissait déjà
        // pour ce voisin (ou s'il était à l'infini), alors on a trouvé un chemin plus rapide pour l'eau !
        if (newT < tFlood.get(voisin)) {
          tFlood.put(voisin, newT);         // On met à jour son étiquette de temps
          vWater.put(voisin, vWaterVoisin); // On enregistre sa nouvelle vitesse d'impact

          // On ajoute le voisin dans la file à priorité. S'il y était déjà,
          // Java va le repositionner (grâce à sa nouvelle valeur tFlood plus petite).
          priorityQueue.add(voisin);
        }
      }
    }

    // 6. FORMATAGE DU RÉSULTAT
    // L'énoncé demande probablement de renvoyer les lieux par ordre chronologique d'inondation.
    // On utilise un TreeMap qui trie automatiquement ses clés selon le Comparator fourni.
    // Le thenComparingLong est une excellente sécurité pour départager deux lieux inondés à la milliseconde près.
    Map<Localisation, Double> result = new TreeMap<>(
        Comparator.comparingDouble((Localisation loc) ->
            tFlood.get(loc)).thenComparingLong((Localisation loc) -> loc.getId())
    );

    // On filtre pour ne garder QUE les lieux qui ont réellement été touchés par l'eau.
    // Si tFlood vaut toujours MAX_VALUE, c'est que l'eau n'y est jamais arrivée (ex: sommet d'une montagne).
    for (Entry<Localisation, Double> localisationDoubleEntry : tFlood.entrySet()) {
      if (localisationDoubleEntry.getValue() < Double.MAX_VALUE) {
        result.put(localisationDoubleEntry.getKey(), localisationDoubleEntry.getValue());
      }
    }

    return result;
  }


  /**
   * Trouve le chemin d'évacuation le plus rapide pour échapper à la crue.
   * ALGORITHME UTILISÉ : Algorithme de Dijkstra (avec contrainte de temps dynamique).
   * Le "poids" des arêtes est le TEMPS de trajet en voiture.
   * * @param idOrigin Le point de départ (où se trouve la personne).
   * @param idEvacuation Le point d'arrivée sécurisé (le refuge).
   * @param vVehicule La vitesse de la voiture de la personne.
   * @param tFlood La chronologie de la crue calculée précédemment (à quelle heure l'eau arrive à chaque point).
   */
  public Deque<Localisation> trouverCheminDEvacuationLePlusCourt(long idOrigin, long idEvacuation,
      double vVehicule, Map<Localisation, Double> tFlood) {

    // 1. VÉRIFICATIONS DE BASE
    Localisation origin = localisationsMap.get(idOrigin);
    Localisation destination = localisationsMap.get(idEvacuation);

    if (origin == null || destination == null) {
      throw new RuntimeException("Localisation introuvable.");
    }

    if (origin.equals(destination)) {
      Deque<Localisation> path = new ArrayDeque<>();
      path.add(origin);
      return path;
    }

    // 2. INITIALISATION DU MOTEUR DE DIJKSTRA
    Map<Localisation, Double> time = new HashMap<>(); // Le temps record pour atteindre chaque lieu en voiture
    Map<Localisation, Localisation> parent = new HashMap<>(); // Le fil d'Ariane pour reconstruire la route

    // Le SET des "nœuds finalisés".
    // Dans Dijkstra, une fois qu'on sort un nœud de la file à priorité, on est MATHÉMATIQUEMENT SÛR
    // d'avoir trouvé le chemin le plus court vers lui (car il n'y a pas de temps négatif).
    // Ce set empêchera de le recalculer inutilement.
    Set<Localisation> visited = new HashSet<>();

    // Initialisation classique de Dijkstra : Tout le monde est à une distance infinie.
    for (Localisation loc : localisationsMap.values()) {
      time.put(loc, Double.MAX_VALUE);
    }
    time.put(origin, 0.0); // Sauf moi, je suis à 0 minute de moi-même.

    // La salle d'attente VIP (File à priorité) triée par temps de trajet en voiture le plus court
    PriorityQueue<Localisation> noeudAExplorer = new PriorityQueue<>(
        Comparator.comparingDouble(time::get)
    );
    noeudAExplorer.add(origin);

    // 3. LA BOUCLE D'EXPLORATION
    while (!noeudAExplorer.isEmpty()) {

      Localisation current = noeudAExplorer.poll(); // On prend le carrefour le plus proche en temps

      // SÉCURITÉ ET OPTIMISATION : Si ce carrefour a déjà été "finalisé" lors d'un tour précédent,
      // on le passe. Cela évite les boucles infinies.
      if (visited.contains(current)) {
        continue;
      }

      // On le marque officiellement comme "chemin le plus court définitif"
      visited.add(current);

      List<Arc> arcs = getArcsSortants(current);

      // 4. ÉVALUATION DES ROUTES POSSIBLES
      for (Arc arc : arcs) {
        Localisation voisin = arc.getTarget();

        //  Optimisation : Inutile d'évaluer une route vers un carrefour déjà finalisé
        if (visited.contains(voisin)) {
          continue;
        }

        // Temps de parcours de cette route précise = Distance / Vitesse de la voiture
        double travelTime = arc.getDist() / vVehicule;

        // L'heure à laquelle ma voiture arrivera au prochain carrefour
        double newTime = time.get(current) + travelTime;

        // L'heure à laquelle la CRUE arrivera à ce même carrefour
        Double floodTime = tFlood.get(voisin);

        // LE CŒUR DE LA LOGIQUE MÉTIER DE L'EXAMEN : LA COURSE CONTRE LA MONTRE
        // Je ne peux emprunter cette route QUE SI :
        // 1. L'eau n'y arrive jamais (floodTime == null, s'il n'était pas dans la Map précédente)
        // 2. OU ALORS, ma voiture arrive AVANT l'eau (newTime < floodTime)
        if (floodTime == null || newTime < floodTime) {

          // RELAXATION DE L'ARÊTE (Classique Dijkstra)
          // Si cette route valide me permet d'arriver plus vite que l'ancien record de ce carrefour :
          if (newTime < time.get(voisin)) {
            time.put(voisin, newTime); // Nouveau record battu !
            parent.put(voisin, current); // Je note d'où je viens
            noeudAExplorer.add(voisin); // Je remets le voisin dans la file pour l'explorer plus tard
          }
        }
      }
    }

    // 5. VALIDATION ET RECONSTRUCTION
    // Si la boucle se termine et que la destination n'a aucun parent, c'est que toutes
    // les routes pour y aller étaient soit inondées, soit trop lentes pour fuir l'eau.
    if (!parent.containsKey(destination) && !origin.equals(destination)) {
      throw new RuntimeException("Aucun chemin d'évacuation trouvé. L'eau vous a rattrapé !");
    }

    // Sinon, on remonte le fil d'Ariane.
    Deque<Localisation> path = reconstructPath(destination, parent);
    return path;
  }


  // CHARGEMENT CSV (nodes)
  private void loadLocalisations() {
    try (BufferedReader br = new BufferedReader(new FileReader(this.localisationsFile))) {
      String line;
      br.readLine(); // Ignorer en-tête

      // Lire toutes les lignes du fichier
      while ((line = br.readLine()) != null) {
        String[] datas = line.split(",");

        // Parse les données (attention à l'ordre des colonnes du fichier)
        long id = Long.parseLong(datas[0].trim());
        String name = datas[1].trim();
        double latitude = Double.parseDouble(datas[2].trim());
        double longitude = Double.parseDouble(datas[3].trim());
        double altitude = Double.parseDouble(datas[4].trim());

        // Créer la localisation
        Localisation loc = new Localisation(id, name, latitude, longitude, altitude);
        // Stocker dans la map pour accès rapide par id
        localisationsMap.put(id, loc);
        // Initialiser la liste d'adjacence pour cette localisation
        listeAdjacence.put(loc, new ArrayList<>());
      }

    } catch (IOException e) {
      throw new RuntimeException("Erreur lors du chargement des localisations.", e);
    }
  }


  //CHARGEMENT CSV (edges)
  private void loadArcs() {
    try (BufferedReader br = new BufferedReader(new FileReader(this.roadsFile))) {
      String line;
      br.readLine(); // Ignorer l'en-tête

      // Lire toutes les lignes du fichier
      while ((line = br.readLine()) != null) {
        String[] datas = line.split(",");

        long sourceId = Long.parseLong(datas[0].trim());
        long targetId = Long.parseLong(datas[1].trim());
        double distance = Double.parseDouble(datas[2].trim());
        String streetName = datas[3].trim();

        // Récupérer les objets Localisation depuis la map
        Localisation source = localisationsMap.get(sourceId);
        Localisation target = localisationsMap.get(targetId);

        // Vérifier que les deux localisations existent
        if (source == null) {
          throw new RuntimeException(
              "Source introuvable: " + sourceId + " pour l'arc: " + line + ".");
        }

        if (target == null) {
          throw new RuntimeException(
              "Destination introuvable: " + targetId + " pour l'arc: " + line + ".");
        }

        // Créer l'arc (sens source -> target)
        Arc arcSortant = new Arc(source, target, distance, streetName);

        // Ajouter l'arc à la liste d'adjacence de la source
        listeAdjacence.get(source).add(arcSortant);
      }

    } catch (IOException e) {
      throw new RuntimeException("Erreur lors du chargement des arcs.", e);
    }
  }

  // Retourne les arcs sortants (voisins directs)
  private List<Arc> getArcsSortants(Localisation l) {
    return listeAdjacence.getOrDefault(l, new ArrayList<>());
  }

  // Reconstruction de chemin via map parent
  private Deque<Localisation> reconstructPath(Localisation destination, Map<Localisation, Localisation> parent) {
    Deque<Localisation> path = new ArrayDeque<>();
    Localisation current = destination;
    while (current != null) {
      path.addFirst(current);
      current = parent.get(current);
    }
    return path;
  }


}
