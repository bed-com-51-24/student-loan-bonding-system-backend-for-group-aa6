package mw.loancompliance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSchemas("ApiError", apiErrorSchema()));
    }

    @Bean
    public GlobalOperationCustomizer documentCommonErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            addErrorResponse(responses, "400",
                    "Invalid request. Check the request body, path or query parameters, and required values.",
                    "Bad Request", "Request validation failed. Check the supplied fields and values.");
            addErrorResponse(responses, "401",
                    "Authentication is required. The bearer token is missing, invalid, or expired.",
                    "Unauthorized", "A valid bearer token is required to access this endpoint.");
            addErrorResponse(responses, "403",
                    "The authenticated account does not have the required role or access to this resource.",
                    "Forbidden", "Your account is not allowed to perform this operation.");
            addErrorResponse(responses, "404",
                    "The requested student, account, organization, employment record, notification, or other resource was not found.",
                    "Not Found", "The requested resource was not found.");
            addErrorResponse(responses, "409",
                    "The request conflicts with current data or a business rule, such as a duplicate record or an unavailable operation.",
                    "Conflict", "The request conflicts with the current state of the resource.");
            addErrorResponse(responses, "500",
                    "An unexpected server or database error occurred. Use the response timestamp and request path when reporting the issue.",
                    "Internal Server Error", "An unexpected error occurred while processing the request.");
            return operation;
        };
    }

    private Schema<?> apiErrorSchema() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema().description("Problem type URI; usually about:blank."))
                .addProperty("title", new StringSchema().description("Short HTTP error title."))
                .addProperty("status", new IntegerSchema().description("HTTP status code."))
                .addProperty("detail", new StringSchema().description("Human-readable explanation of this error."))
                .addProperty("instance", new StringSchema().description("Request path associated with the error."))
                .addProperty("error", new StringSchema().description("Error message used by some existing validation responses."));
    }

    private void addErrorResponse(ApiResponses responses, String status, String description,
            String title, String detail) {
        if (responses.containsKey(status)) {
            return;
        }

        Example example = new Example()
                .summary(title)
                .value(java.util.Map.of(
                        "type", "about:blank",
                        "title", title,
                        "status", Integer.parseInt(status),
                        "detail", detail,
                        "instance", "/api/..."));
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/ApiError");
        MediaType json = new MediaType().schema(schema).addExamples("example", example);
        Content content = new Content()
                .addMediaType("application/problem+json", json)
                .addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiError"))
                        .addExamples("example", example));
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(content));
    }
}
