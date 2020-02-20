package com.videoamp.cleanroom.queryanalyzer;

import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.zetasql.*;
import com.google.zetasql.resolvedast.ResolvedNodes;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import static com.google.zetasql.ZetaSQLResolvedNodeKind.ResolvedNodeKind;

public class HelloWorld extends AbstractVerticle {

    public void start() {
        final Router router = Router.router(vertx);
        router
                .get("/cleanroom/health")
                .handler(
                        context -> {
                            context.response().setStatusCode(200).end();
                        });

        JsonFormat.Printer jsonPrinter = JsonFormat.printer();

        router.route("/cleanroom/query").handler(BodyHandler.create());
        router
                .post("/cleanroom/query")
                .handler(
                        context -> {
                            JsonObject payload = context.getBodyAsJson();
                            String sqlStatement = payload.getString("sql");

                            BigQueryCatalog bigQueryCatalog = new BigQueryCatalog("default_catalog");
                            bigQueryCatalog.addZetaSQLFunctions(new ZetaSQLBuiltinFunctionOptions());

                            LanguageOptions languageOptions = new LanguageOptions();
                            languageOptions.enableMaximumLanguageFeatures();
                            languageOptions.setLanguageVersion(ZetaSQLOptions.LanguageVersion.VERSION_CURRENT);
                            languageOptions.setProductMode(ZetaSQLOptions.ProductMode.PRODUCT_EXTERNAL);
                            languageOptions.setSupportsAllStatementKinds();

                            AnalyzerOptions analyzerOptions = new AnalyzerOptions();
                            analyzerOptions.setLanguageOptions(languageOptions);
                            Analyzer analyzer = new Analyzer(analyzerOptions, bigQueryCatalog);

                            Analyzer.extractTableNamesFromStatement(sqlStatement).stream()
                                    .map(
                                            nameSegmentList -> {
                                                String[] nameSegmentArray = new String[nameSegmentList.size()];
                                                nameSegmentList.toArray(nameSegmentArray);
                                                return nameSegmentArray;
                                            })
                                    .forEach(
                                            nameSegmentArray -> {
                                                SimpleTable resolvedTable =
                                                        bigQueryCatalog.lookupTable(nameSegmentArray);
                                                SimpleCatalog catalog =
                                                        bigQueryCatalog.getOrCreateCatalogForTable(nameSegmentArray);
                                                catalog.addSimpleTable(resolvedTable);
                                            });

                            ParseResumeLocation input = new ParseResumeLocation(sqlStatement);
                            while (ParseResumeLocationUtils.shouldResume(input)) {
                                ResolvedNodes.ResolvedStatement resolvedStatement =
                                        analyzer.analyzeNextStatement(input);
                                try {
                                    context
                                            .response()
                                            .setChunked(true)
                                            .putHeader("Content-Type", "text/plain")
                                            .setStatusCode(200)
                                            .write(jsonPrinter.print(resolvedStatement.serialize(null)));
                                } catch (InvalidProtocolBufferException e) {
                                    context.response().setChunked(true).setStatusCode(500).end(e.getMessage());
                                }

                                if (resolvedStatement.nodeKind() == ResolvedNodeKind.RESOLVED_CREATE_FUNCTION_STMT) {
                                    bigQueryCatalog.addFunction((ResolvedNodes.ResolvedCreateFunctionStmt) resolvedStatement);
                                }
                            }
                            context.response().setChunked(true).end();
                        });

        final HttpServer server = vertx.createHttpServer();
        server.requestStream().handler(router::accept);
        server.listen(8080);
    }
}
