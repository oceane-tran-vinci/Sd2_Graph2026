/**
 * ARC (Edge dans un graphe)
 *
 * Idée générale :
 * Un Arc représente une connexion orientée entre deux Localisations.
 * C’est une "route" ou "liaison" utilisée par tous les algorithmes de parcours.
 *
 * IMPORTANT :
 * Dans un langage de graphes, un Arc = Edge.
 * Donc si le prof change les noms, Arc peut devenir Edge sans changer la logique.
 *
 * Utilisation dans le projet :
 * - BFS : pour trouver les voisins
 * - Dijkstra : pour calculer un coût (distance / temps)
 * - Crue : pour calculer la propagation via la pente
 */
public class Arc {

  /**
   * Point de départ de l’arc
   * → utilisé pour construire la liste d’adjacence :
   *   source -> liste des arcs sortants
   */
  private Localisation source;

  /**
   * Point d’arrivée de l’arc
   * → permet de définir les voisins dans les algorithmes de graphe
   */
  private Localisation target;

  /**
   * Distance entre source et target
   * → utilisé dans :
   *   - Dijkstra (coût de déplacement)
   *   - temps de trajet (distance / vitesse)
   */
  private double dist;

  /**
   * Nom de la rue
   * → donnée “informatique descriptive”, pas utilisée directement dans les algos
   * mais utile pour affichage / debug / compréhension du graphe
   */
  private String streetName;

  /**
   * Constructeur
   *
   * Ici on "matérialise une arête du graphe".
   * Chaque arc est créé lors du chargement du fichier roads.csv.
   */
  public Arc(Localisation source, Localisation target, double dist, String streetName) {
    this.source = source;
    this.target = target;
    this.dist = dist;
    this.streetName = streetName;
  }

  // =========================
  // GETTERS / SETTERS
  // =========================

  public Localisation getSource() {
    return source;
  }

  public void setSource(Localisation source) {
    this.source = source;
  }

  public Localisation getTarget() {
    return target;
  }

  public void setTarget(Localisation target) {
    this.target = target;
  }

  public double getDist() {
    return dist;
  }

  public void setDist(double dist) {
    this.dist = dist;
  }

  public String getStreetName() {
    return streetName;
  }

  public void setStreetName(String streetName) {
    this.streetName = streetName;
  }

  /**
   * Calcul de la pente entre source et target
   *
   * Pourquoi ça existe ?
   * → utilisé UNIQUEMENT dans la simulation de crue (Dijkstra 2)
   *
   * Logique :
   * - si pente positive → descente (l’eau accélère)
   * - si pente négative → montée (l’eau ralentit)
   *
   * C’est donc un facteur de variation de vitesse dans :
   * determinerChronologieDeLaCrue()
   */
  public double getPente() {
    return (source.getAltitude() - target.getAltitude()) / dist;
  }
}