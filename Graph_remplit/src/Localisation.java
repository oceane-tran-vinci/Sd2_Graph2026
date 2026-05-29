import java.util.Objects;

public class Localisation {

  private long id;
  private String name;
  private double latitude;
  private double longitude;
  private double altitude;

  public Localisation(long id, String name, double latitude, double longitude, double altitude) {
    this.id = id;
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
  }

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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Localisation that = (Localisation) o;
    return id == that.id && Double.compare(latitude, that.latitude) == 0
        && Double.compare(longitude, that.longitude) == 0
        && Double.compare(altitude, that.altitude) == 0 && Objects.equals(name,
        that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, latitude, longitude, altitude);
  }
}
