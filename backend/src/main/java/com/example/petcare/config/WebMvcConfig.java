package com.example.petcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Path.of("uploads").toAbsolutePath().normalize().toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        String frontendDistPathFromBackend = directoryUri(Path.of("..", "frontend", "dist"));
        String frontendDistPathFromRoot = directoryUri(Path.of("frontend", "dist"));
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(
                        frontendDistPathFromBackend + "assets/",
                        frontendDistPathFromRoot + "assets/",
                        "classpath:/static/assets/"
                );
        registry.addResourceHandler("/pet-avatars/**")
                .addResourceLocations(
                        frontendDistPathFromBackend + "pet-avatars/",
                        frontendDistPathFromRoot + "pet-avatars/",
                        directoryUri(Path.of("..", "frontend", "public", "pet-avatars")),
                        directoryUri(Path.of("frontend", "public", "pet-avatars")),
                        "classpath:/static/pet-avatars/"
                );
        registry.addResourceHandler("/*.*")
                .addResourceLocations(frontendDistPathFromBackend, frontendDistPathFromRoot, "classpath:/static/");
    }

    private String directoryUri(Path path) {
        String uri = path.toAbsolutePath().normalize().toUri().toString();
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        return uri;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/{path:^(?!api|uploads|assets|pet-avatars|.*\\..*$).*$}")
                .setViewName("forward:/index.html");
        registry.addViewController("/{first:^(?!api|uploads|assets|pet-avatars).*$}/{second:^(?!.*\\..*$).*$}")
                .setViewName("forward:/index.html");
    }
}
