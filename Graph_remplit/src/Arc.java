
public class Arc {

  //Localisation et non Long car source target = id Localisation
  private Localisation source;
  private Localisation target;
  private double dist;
  private String streetName;

  public Arc(Localisation source, Localisation target, double dist, String streetName) {

    this.source = source;
    this.target = target;
    this.dist = dist;
    this.streetName = streetName;
  }

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

  public double getPente() {
    return (source.getAltitude() - target.getAltitude()) / dist;
  }
}