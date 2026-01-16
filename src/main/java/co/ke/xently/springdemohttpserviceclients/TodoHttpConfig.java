package co.ke.xently.springdemohttpserviceclients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.annotation.*;
import org.springframework.web.service.registry.ImportHttpServices;

import java.io.IOException;
import java.util.List;

record Post(long id, String title) {
}

record CreatePostRequest(String title) {
}

record UpdatePostRequest(String title) {
}

@HttpExchange(value = "/posts")
interface PostClient {
    @GetExchange
    List<Post> getPosts();

    @GetExchange("/{id}")
    Post getPost(@PathVariable long id);

    @PostExchange
    Post create(@RequestBody CreatePostRequest post);

    @PutExchange("/{id}")
    Post update(@PathVariable long id, @RequestBody UpdatePostRequest post);

    @DeleteExchange("/{id}")
    void delete(@PathVariable long id);
}

@RestController
@RequestMapping("/posts")
class PostController {
    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostClient postClient;

    PostController(PostClient postClient) {
        this.postClient = postClient;
    }

    @GetMapping
    ResponseEntity<List<Post>> getPosts() {
        try {
            var posts = postClient.getPosts();
            return ResponseEntity.ok(posts);
        } catch (HttpClientErrorException e) {
            log.error("Error getting posts", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    ResponseEntity<Post> getPost(@PathVariable long id) {
        try {
            var posts = postClient.getPost(id);
            return ResponseEntity.ok(posts);
        } catch (HttpClientErrorException e) {
            log.error("Error getting post", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    ResponseEntity<Post> updatePost(@PathVariable long id, @RequestBody UpdatePostRequest post) {
        try {
            var posts = postClient.update(id, post);
            return ResponseEntity.ok(posts);
        } catch (HttpClientErrorException e) {
            log.error("Error updating post", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Post> deletePost(@PathVariable long id) {
        try {
            postClient.delete(id);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.error("Error deleting post", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    ResponseEntity<Post> createPost(@RequestBody CreatePostRequest post) {
        try {
            var posts = postClient.create(post);
            return ResponseEntity.ok(posts);
        } catch (HttpClientErrorException e) {
            log.error("Error creating post", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

@Configuration
@ImportHttpServices(group = "posts", basePackageClasses = Application.class)
class PostHttpConfig {
    private static final Logger log = LoggerFactory.getLogger(PostHttpConfig.class);

    @Bean
    RestClientHttpServiceGroupConfigurer restClientHttpServiceGroupConfigurer() {
        return groups -> groups.filterByName("posts")
                .forEachClient((_, clientBuilder) -> clientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                        .requestInterceptors(clientHttpRequestInterceptors -> {
                            clientHttpRequestInterceptors.add(new BasicAuthenticationInterceptor("username", "password"));
                            clientHttpRequestInterceptors.addLast(new RequestLogInterceptor(log));
                        }));
    }

    private record RequestLogInterceptor(Logger logger) implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            logger.info("Request: {} {}", request.getMethod(), request.getURI());
            request.getHeaders()
                    .forEach((name, values) -> logger.info("{}: {}", name, String.join(", ", values)));
            logger.info("Body: {}", new String(body));
            return execution.execute(request, body);
        }
    }
}
