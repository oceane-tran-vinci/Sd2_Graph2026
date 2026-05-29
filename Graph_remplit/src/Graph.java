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
   * =========================================
   * BFS FLOOD FILL (propagation d'inondation)
   * =========================================
   *
   * Algorithme : BFS (parcours en largeur)
   *
   * Idée :
   * - On part des sources d'inondation
   * - On propage vers les voisins si condition altitude OK
   *
   * Structures utilisées :
   * - Queue → exploration BFS
   * - Set → éviter revisite (cycle)
   * - List → garder ordre de propagation
   */
  public Localisation[] determinerZoneInondee(long[] idsOrigin, double epsilon) {
    // Check des ids de départ
    if (idsOrigin == null || idsOrigin.length == 0) {
      return new Localisation[0];
    }

    Queue<Localisation> queue = new ArrayDeque<>(); // BFS Queue pour visiter les voisins
    Set<Localisation> flooded = new HashSet<>(); // Set pour garder les localisations visitées
    List<Localisation> visitedOrder = new ArrayList<>(); // List pour garder l'ordre de parcours (BFS) (utile pour debug / résultat)

    // Initialisation des sources d’inondation
    for (long id : idsOrigin) {
      Localisation source = localisationsMap.get(id);
      // Ajout uniquement si valide + pas déjà ajouté
      if (source != null && flooded.add(source)) { // Si c'est une source -> elle est toujours inondée donc on l'ajoute
        queue.add(source);
        visitedOrder.add(source);
      }
    }

    // On cherche les voisins inondés, BFS classique
    while (!queue.isEmpty()) {
      Localisation currentLoc = queue.poll(); // FIFO BFS
      List<Arc> arcsSortants = getArcsSortants(currentLoc); // Tous les arcs sortants = voisins directs

      for (Arc arc : arcsSortants) {
        Localisation voisin = arc.getTarget();
        // Condition physique : l'eau monte seulement si altitude accessible
        if (voisin.getAltitude() <= currentLoc.getAltitude() + epsilon && flooded.add(voisin)) {
          queue.add(voisin);
          visitedOrder.add(voisin);
        }
      }
    }

    return visitedOrder.toArray(new Localisation[0]);
  }

  /**
   * ==========================================
   * BFS SHORTEST PATH (avec obstacles inondés)
   * ==========================================
   *
   * Algorithme : BFS classique + filtrage de nœuds
   *
   * Ajout important :
   * - Map parent → reconstruction du chemin
   *
   * Structures :
   * - Queue → exploration BFS
   * - Set visited → éviter cycles
   * - Map parent → remonter chemin final
   */
  public Deque<Localisation> trouverCheminLePlusCourtPourContournerLaZoneInondee(long idOrigin,
      long idDestination, Localisation[] floodedZone) {

    // Récupérer les points de départ et d'arrivée
    Localisation origin = localisationsMap.get(idOrigin);
    Localisation destination = localisationsMap.get(idDestination);

    if (origin == null || destination == null) {
      throw new RuntimeException("Localisation introuvable.");
    }

    if (origin.equals(destination)) {
      Deque<Localisation> path = new ArrayDeque<>();
      path.add(origin);
      return path;
    }


    // Convertir floodedZone en Set pour accès rapide O(1) aux zones interdites
    Set<Localisation> floodedZoneSet = new HashSet<>(Arrays.asList(floodedZone));

    // Vérifier si origine ou destination sont inondés
    if (floodedZoneSet.contains(origin)) {
      throw new RuntimeException("Le point d'origine " + idOrigin + " est inondé !");
    }
    if (floodedZoneSet.contains(destination)) {
      throw new RuntimeException("Le point de destination " + idDestination + " est inondé !");
    }

    // BFS structures
    Queue<Localisation> queue = new ArrayDeque<>(); // Queue pour BFS
    Set<Localisation> visited = new HashSet<>(); // Set pour les nœuds visités
    Map<Localisation, Localisation> parent = new HashMap<>(); // Pour reconstruire le chemin
    // Initialisation avec le point d'origine
    queue.add(origin);
    visited.add(origin);
    parent.put(origin, null);

    boolean found = false;

    while (!queue.isEmpty() && !found) {
      Localisation current = queue.poll();
      List<Arc> arcsSortants = getArcsSortants(
          current); // Parcourir tous les arcs sortants du noeud courant

      for (Arc arc : arcsSortants) {
        Localisation voisins = arc.getTarget(); // Définir le voisin

        // Skip si déjà visité OU zone inondée
        if (visited.contains(voisins) || floodedZoneSet.contains(voisins)) {
          continue;
        }

        // Marquer comme visité et enregistrer le parent
        queue.add(voisins);
        visited.add(voisins);
        parent.put(voisins, current);

        // arrêt immédiat si destination atteinte
        if (voisins.equals(destination)) {
          found = true;
          break;
        }
      }
    }

    // Reconstruire le chemin origine -> destination
    if (!found) {
      throw new RuntimeException("Pas de chemin de " + idOrigin + " à " + idDestination
          + " évitant la zone inondée.");
    }

    Deque<Localisation> path = reconstructPath(destination, parent);

    return path;
  }


  /**
   * ==========================================
   * DIJKSTRA (propagation crue avec coûts dynamiques)
   * ==========================================
   *
   * Algorithme : Dijkstra modifié
   *
   * Particularité :
   * - coût dépend du temps + vitesse variable
   *
   * Structures :
   * - PriorityQueue → toujours le plus petit temps
   * - Map tFlood → distance (temps inondation)
   * - Map vWater → état dynamique du système
   */
  public Map<Localisation, Double> determinerChronologieDeLaCrue(long[] idsOrigin,
      double vWaterInit, double k) {
    if (idsOrigin == null || idsOrigin.length == 0) {
      throw new IllegalArgumentException("Aucune source trouvée.");
    }
    if (vWaterInit < 0) {
      throw new IllegalArgumentException("La vitesse de l'eau ne peut être négative");
    }

    // on stocke le temps d'inondation et la vitesse de l'eau par loc
    Map<Localisation, Double> tFlood = new HashMap<>(); // Temps inondation de chaque lieu
    Map<Localisation, Double> vWater = new HashMap<>(); // Vitesse de l'eau de chaque lieu

    // initialisation : tous infiniment loin
    localisationsMap.values().forEach((l) -> tFlood.put(l, Double.MAX_VALUE));

    // PriorityQueue = Dijkstra
    PriorityQueue<Localisation> priorityQueue = new PriorityQueue<>(
        Comparator.comparingDouble(tFlood::get));

    // initialisation sources
    for (long id : idsOrigin) {
      Localisation source = localisationsMap.get(id);
      if (source != null) {
        vWater.put(source, vWaterInit);
        tFlood.put(source, 0.0);
        priorityQueue.add(source);
      }
    }

    while (!priorityQueue.isEmpty()) {
      Localisation currentLoc = priorityQueue.poll(); // on prend le 1er

      double currentTime = tFlood.get(currentLoc);
      double currentVitesse = vWater.get(currentLoc);

      List<Arc> arcsSortants = getArcsSortants(currentLoc);

      for (Arc arcsSortant : arcsSortants) {
        Localisation voisin = arcsSortant.getTarget();

        double pente = arcsSortant.getPente();

        // vitesse mise à jour selon pente
        double vWaterVoisin = currentVitesse + (k * pente);
        if (vWaterVoisin <= 0) {
          continue;
        }

        double tArc = arcsSortant.getDist() / vWaterVoisin;
        double newT = currentTime + tArc;

        // relaxation Dijkstra
        if (newT < tFlood.get(voisin)) {
          tFlood.put(voisin, newT);
          vWater.put(voisin, vWaterVoisin);
          priorityQueue.add(voisin);
        }
      }
    }

    Map<Localisation, Double> result = new TreeMap<>(
        Comparator.comparingDouble((Localisation loc) ->
            tFlood.get(loc)).thenComparingLong((Localisation loc) -> loc.getId())
    );

    for (Entry<Localisation, Double> localisationDoubleEntry : tFlood.entrySet()) {
      if (localisationDoubleEntry.getValue() < Double.MAX_VALUE) {
        result.put(localisationDoubleEntry.getKey(), localisationDoubleEntry.getValue());
      }
    }
    return result;
  }

  /**
   * ==========================================
   * DIJKSTRA + CONTRAINTE (évacuation)
   * ==========================================
   *
   * Variante :
   * - Dijkstra classique
   * - + contrainte : ne pas arriver après inondation
   */
  public Deque<Localisation> trouverCheminDEvacuationLePlusCourt(long idOrigin, long idEvacuation,
      double vVehicule, Map<Localisation, Double> tFlood) {

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

    // Maps pour Dijkstra
    Map<Localisation, Double> time = new HashMap<>();
    Map<Localisation, Localisation> parent = new HashMap<>();

    // SET POUR LES NŒUDS VISITÉS
    Set<Localisation> visited = new HashSet<>();

    // Initialiser tous les temps
    for (Localisation loc : localisationsMap.values()) {
      time.put(loc, Double.MAX_VALUE);
    }
    time.put(origin, 0.0);

    PriorityQueue<Localisation> noeudAExplorer = new PriorityQueue<>(
        Comparator.comparingDouble(time::get)
    );
    noeudAExplorer.add(origin);

    while (!noeudAExplorer.isEmpty()) {

      Localisation current = noeudAExplorer.poll();

      // Skip si déjà visité
      if (visited.contains(current)) {
        continue;
      }

      // Marquer comme visité une fois traité
      visited.add(current);

      // Traiter les voisins
      List<Arc> arcs = getArcsSortants(current);
      for (Arc arc : arcs) {

        Localisation voisin = arc.getTarget();

        //  Skip les voisins déjà visités
        if (visited.contains(voisin)) {
          continue;
        }

        double travelTime = arc.getDist() / vVehicule;
        double newTime = time.get(current) + travelTime;
        Double floodTime = tFlood.get(voisin);

        // Vérifier les conditions d'inondation
        if (floodTime == null || newTime < floodTime) {

          // Ajouter seulement si meilleur chemin ET pas visité
          if (newTime < time.get(voisin)) {
            time.put(voisin, newTime);
            parent.put(voisin, current);
            noeudAExplorer.add(voisin);
          }
        }
      }
    }

    if (!parent.containsKey(destination) && !origin.equals(destination)) {
      throw new RuntimeException("Aucun chemin d'évacuation trouvé.");
    }

    Deque<Localisation> path = reconstructPath(destination, parent);
    return path;
  }


  /**
   * ======================
   * CHARGEMENT CSV (nodes)
   * ======================
   */
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
