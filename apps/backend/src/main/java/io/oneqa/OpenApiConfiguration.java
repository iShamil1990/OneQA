package io.oneqa;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;
@Configuration
public class OpenApiConfiguration{
@Bean OpenAPI api(){return new OpenAPI().info(new Info().title("OneQA API").version("v1"));}}
