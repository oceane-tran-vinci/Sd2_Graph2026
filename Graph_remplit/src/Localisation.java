import java.util.Objects;

/**
 * LOCALISATION (Node / Vertex dans un graphe)
 *
 * Idée générale :
 * Une Localisation représente un sommet du graphe.
 * C’est un point géographique (ville, croisement, lieu…)
 *
 * IMPORTANT EXAM :
 * - Localisation = Node = Vertex
 * - C’est ce que les algorithmes parcourent (BFS / Dijkstra)
 *
 * Elle ne contient PAS de logique de graphe.
 * Elle contient uniquement des données.
 */
public class Localisation {

  /**
   * Identifiant unique du nœud
   *
   * Rôle CRUCIAL :
   * - utilisé pour identifier une localisation dans le fichier CSV
   * - permet de retrouver rapidement un node dans Map<Long, Localisation>
   */
  private long id;

  /**
   * Nom du lieu (ville, rue, point d’intérêt)
   * → utilisé uniquement pour affichage / debug
   */
  private String name;

  /**
   * Coordonnées géographiques
   * → pas utilisées directement dans les algorithmes ici
   * mais utiles pour extension (distance réelle, GPS, etc.)
   */
  private double latitude;
  private double longitude;

  /**
   * Altitude
   *
   * UTILISATION IMPORTANTE :
   * - propagation de la crue (BFS modifié)
   * - comparaison avec epsilon
   * - influence indirecte sur les chemins
   */
  private double altitude;

  /**
   * Constructeur
   *
   * Une Localisation est créée lors du chargement du CSV.
   * Elle devient ensuite un nœud du graphe stocké dans :
   * Map<Long, Localisation>
   */
  public Localisation(long id, String name, double latitude, double longitude, double altitude) {
    this.id = id;
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
  }

  // =========================
  // GETTERS / SETTERS
  // =========================

  public long getId() {
    return id;
  }

  public String getNom() {
    return name;
  }

  public void setNom(String name) {
    this.name = name;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getAltitude() {
    return altitude;
  }

  public void setAltitude(double altitude) {
    this.altitude = altitude;
  }

  // ==========================================================
  // IMPORTANT : equals + hashCode (TRÈS IMPORTANT EN GRAPHE)
  // ==========================================================

  /**
   * equals()
   *
   * Pourquoi on en a besoin ?
   * → Les Localisation sont utilisées comme clés dans :
   *   - HashMap (adjacence, distances)
   *   - HashSet (visited)
   *   - PriorityQueue indirectement via maps
   *
   * Donc deux Localisations doivent être comparées correctement.
   *
   * Ici on considère deux nodes égaux si :
   * → mêmes id + mêmes coordonnées + même nom
   *
   * (c’est une définition "forte", pas juste id)
   */
  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Localisation that = (Localisation) o;

    return id == that.id
        && Double.compare(latitude, that.latitude) == 0
        && Double.compare(longitude, that.longitude) == 0
        && Double.compare(altitude, that.altitude) == 0
        && Objects.equals(name, that.name);
  }

  /**
   * hashCode()
   *
   * Pourquoi c’est obligatoire ?
   * → HashMap / HashSet utilisent hashCode pour retrouver rapidement un objet
   *
   * Règle JAVA :
   * si equals() est redéfini → hashCode DOIT l’être aussi
   *
   * Sinon :
   * → bugs dans BFS / Dijkstra (visited cassé, doublons, etc.)
   */
  @Override
  public int hashCode() {
    return Objects.hash(id, name, latitude, longitude, altitude);
  }
}