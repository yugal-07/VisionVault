package Vision_Vault;

import java.io.IOException;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class photo_manager {

  HashMap<String, Photo> db = new HashMap<>();

  public photo_manager() {
    Photo temp = new Photo("img", "001");
    db.put("1", temp);
  }

  @GetMapping("/photos")
  public HashMap<String, Photo> getDB() {
    return db;
  }

  @GetMapping("/photos/{num}")
  public Photo get(@PathVariable String num) {
    Photo p = db.get(num);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return p;
  }

  @PostMapping("/photos")
  public void upload(@RequestPart("data") MultipartFile file) throws IOException {
    Photo p = new Photo();
    String name = file.getOriginalFilename();
    p.setName(name);
    Embedding e = new Embedding();
    p.setId(e.getEmbeddings(name));
    p.setData(file.getBytes());
    db.put(p.getId(), p);
  }

  @DeleteMapping("/photos/{num}")
  public void delete(@PathVariable String num) {
    Photo p = db.remove(num);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }
}
