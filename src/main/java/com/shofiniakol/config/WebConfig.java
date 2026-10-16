package com.shofiniakol.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    /** Arabic is the default; en / fr are served when requested via Accept-Language. */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("ar"));
        resolver.setSupportedLocales(List.of(Locale.forLanguageTag("ar"), Locale.ENGLISH, Locale.FRENCH));
        return resolver;
    }

    /** Serves uploaded images from /media/** */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Path.of(props.getStorage().getDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/**")
                .addResourceLocations(dir.endsWith("/") ? dir : dir + "/")
                .setCachePeriod(86400);
    }
}
