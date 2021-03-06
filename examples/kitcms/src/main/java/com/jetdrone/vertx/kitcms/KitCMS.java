package com.jetdrone.vertx.kitcms;

import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.MimeType;
import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.engine.StringPlaceholderEngine;
import com.jetdrone.vertx.yoke.middleware.*;
import com.jetdrone.vertx.yoke.util.AsyncIterator;
import io.vertx.java.redis.RedisClient;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class KitCMS extends Verticle {

    @Override
    public void start() {
        final Config config = new Config(container.config());
        final EventBus eb = vertx.eventBus();
        final FileSystem fileSystem = vertx.fileSystem();

        final RedisClient db = new RedisClient(eb, Config.REDIS_ADDRESS);

        db.deployModule(container, config.dbServer, config.dbPort);

        final Yoke yoke = new Yoke(this);
        // register jMustache render engine
        yoke.engine(new StringPlaceholderEngine());
        // log the requests
        yoke.use(new Logger());
        // install the pretty error handler middleware
        yoke.use(new ErrorHandler(true));
        // install the favicon middleware
        yoke.use(new Favicon());
        // install custom middleware to identify the domain
        yoke.use(new DomainMiddleware(config));
        // install the static file server
        // note that since we are mounting under /static the root for the static middleware
        // will always be prefixed with /static
        yoke.use("/static", new Static("."));
        // install the BasicAuth middleware
        yoke.use("/admin", new BasicAuth(config.adminUsername, config.adminPassword));
        // install body parser for /admin requests
        yoke.use("/admin", new BodyParser());
        // install router for admin requests
        yoke.use(new Router()
            .get("/admin", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");

                    db.keys(domain.namespace + "&*", new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (!"ok".equals(message.body().getString("status"))) {
                                next.handle(message.body().getString("message"));
                            } else {
                                StringBuilder keys = new StringBuilder();
                                JsonArray json = message.body().getArray("value");

                                for (int i = 0; i < json.size(); i++) {
                                    keys.append("<li data-value=\"");
                                    keys.append(json.get(i));
                                    keys.append("\">");
                                    keys.append(json.get(i));
                                    keys.append("</li>");
                                }

                                request.put("keys", keys);
                                request.response().render("com/jetdrone/vertx/kitcms/views/admin.shtml", next);
                            }
                        }
                    });
                }
            })
            .get("/admin/keys", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");

                    db.keys(domain.namespace + "&*", new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (!"ok".equals(message.body().getString("status"))) {
                                next.handle(message.body().getString("message"));
                            } else {
                                request.response().end(message.body().getString("value"));
                            }
                        }
                    });
                }
            })
            .get("/admin/get", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");
                    String key = request.params().get("key");

                    if (key == null) {
                        request.response().end("Missing key");
                        return;
                    }

                    db.get(domain.namespace + "&" + key, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (!"ok".equals(message.body().getString("status"))) {
                                next.handle(message.body().getString("message"));
                            } else {
                                request.response().setContentType("application/json");
                                // TODO: escape
                                request.response().end("\"" + message.body().getString("value") + "\"");
                            }
                        }
                    });
                }
            })
            .post("/admin/set", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");

                    MultiMap body = request.formAttributes();

                    String key = body.get("key");
                    String value = body.get("value");

                    if (key == null) {
                        request.response().end("Missing key");
                        return;
                    }

                    if (value == null) {
                        request.response().end("Missing value");
                        return;
                    }

                    db.set(domain.namespace + "&" + key, value, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (!"ok".equals(message.body().getString("status"))) {
                                next.handle(message.body().getString("message"));
                            } else {
                                request.response().setContentType("application/json");
                                request.response().end("\"OK\"");
                            }
                        }
                    });
                }
            })
            .post("/admin/unset", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");

                    MultiMap body = request.formAttributes();

                    String key = body.get("key");

                    if (key == null) {
                        request.response().end("Missing key");
                        return;
                    }

                    db.del(domain.namespace + "&" + key, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (!"ok".equals(message.body().getString("status"))) {
                                next.handle(message.body().getString("message"));
                            } else {
                                request.response().setContentType("application/json");
                                request.response().end("\"OK\"");
                            }
                        }
                    });
                }
            })
            .get("/admin/export", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");

                    db.keys(domain.namespace + "&*", new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (!"ok".equals(message.body().getString("status"))) {
                                next.handle(message.body().getString("message"));
                            } else {
                                // need to iterate all json array elements and get from redis
                                new AsyncIterator<Object>(message.body().getArray("value")) {

                                    final JsonArray buffer = new JsonArray();

                                    @Override
                                    public void handle(Object o) {
                                        if (hasNext()) {
                                            final String key = (String) o;
                                            db.get(domain.namespace + "&" + key, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    if (!"ok".equals(message.body().getString("status"))) {
                                                        next.handle(message.body().getString("message"));
                                                    } else {
                                                        JsonObject json = new JsonObject();
                                                        json.putString("key", key);
                                                        json.putString("value", message.body().getString("value"));
                                                        buffer.addObject(json);

                                                        next();
                                                    }
                                                }
                                            });
                                        } else {
                                            YokeResponse response = request.response();

                                            String filename = System.currentTimeMillis() + "_export.kit";

                                            response.putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                                            response.end(buffer);
                                        }
                                    }
                                };
                            }
                        }
                    });
                }
            })
            .post("/admin/import", new Middleware() {
                @Override
                public void handle(final YokeRequest request, final Handler<Object> next) {
                    final Config.Domain domain = request.get("domain");

                    final YokeFileUpload file = request.getFile("file");

                    fileSystem.readFile(file.path(), new AsyncResultHandler<Buffer>() {
                        @Override
                        public void handle(AsyncResult<Buffer> read) {
                            // delete the temp file
                            file.delete();
                            // parse
                            JsonArray json = new JsonArray(read.result().toString());
                            // iterate
                            new AsyncIterator<Object>(json) {
                                @Override
                                public void handle(Object o) {
                                    if (hasNext()) {
                                        final JsonObject json = (JsonObject) o;
                                        db.set(domain.namespace + "&" + json.getString("key"), json.getString("value"), new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> message) {
                                                if (!"ok".equals(message.body().getString("status"))) {
                                                    next.handle(message.body().getString("message"));
                                                } else {
                                                    next();
                                                }
                                            }
                                        });
                                    } else {
                                        request.response().redirect("/admin");
                                    }
                                }
                            };
                        }
                    });
                }
            }));

        // if the request fall through it is a view to render from the db
        yoke.use(new Middleware() {
            @Override
            public void handle(final YokeRequest request, final Handler<Object> next) {
                final Config.Domain domain = request.get("domain");
                final String file = request.path().toLowerCase();

                db.get(domain.namespace + "&" + file, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        if (!"ok".equals(message.body().getString("status"))) {
                            next.handle(message.body().getString("message"));
                        } else {
                            if (message.body().getValue("value") == null) {
                                // if nothing is found on the database proceed with the chain
                                next.handle(null);
                            } else {
                                request.response().setContentType(MimeType.getMime(file, "text/html"));
                                request.response().end(message.body().getString("value"));
                            }
                        }
                    }
                });
            }
        });

        yoke.listen(config.serverPort, config.serverAddress);
        container.logger().info("Vert.x Server listening on " + config.serverAddress + ":" + config.serverPort);
    }
}
