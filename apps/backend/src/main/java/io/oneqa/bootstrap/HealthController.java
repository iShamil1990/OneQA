package io.oneqa.bootstrap;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/system")
public class HealthController{
 @GetMapping("/ping")
 public Map<String,String> ping(){ return Map.of("status","UP","application","OneQA");}
}
