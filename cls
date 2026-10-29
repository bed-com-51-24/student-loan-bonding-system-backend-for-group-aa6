[1mdiff --cc src/main/java/mw/loancompliance/config/SecurityConfig.java[m
[1mindex f16e487,73ec922..0000000[m
[1m--- a/src/main/java/mw/loancompliance/config/SecurityConfig.java[m
[1m+++ b/src/main/java/mw/loancompliance/config/SecurityConfig.java[m
[36m@@@ -41,13 -51,13 +49,22 @@@[m [mpublic class SecurityConfig [m
                                  "/api/auth/forgot-password",[m
                                  "/api/auth/reset-password"[m
                          ).permitAll()[m
[32m++<<<<<<< HEAD[m
[32m +                        .requestMatchers([m
[32m +                                "/error",[m
[32m +                                "/actuator/health/**",[m
[32m++=======[m
[32m+                         .requestMatchers("/error").permitAll()[m
[32m+                         .requestMatchers([m
[32m++>>>>>>> origin/feature/user-module[m
                                  "/swagger-ui.html",[m
                                  "/swagger-ui/**",[m
                                  "/v3/api-docs/**"[m
                          ).permitAll()[m
[32m++<<<<<<< HEAD[m
[32m++=======[m
[32m+                         .requestMatchers("/actuator/health/**").permitAll()[m
[32m++>>>>>>> origin/feature/user-module[m
                          .requestMatchers([m
                                  org.springframework.http.HttpMethod.OPTIONS, "/**"[m
                          ).permitAll()[m
