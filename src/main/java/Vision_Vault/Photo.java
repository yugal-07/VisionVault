package Vision_Vault;

public class Photo {
  private String name;
  private String id;
  private byte[] data;

  public Photo(String name, String id) {
    this.name = name;
    this.id = id;
  }

  public Photo() {
    name = "invalid";
    id = "000";
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(String s) {
    id = s;
  }

  public void setName(String s) {
    name = s;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return data;
  }
}
