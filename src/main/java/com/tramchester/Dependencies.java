package com.tramchester;


import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.FetchInstanceMetadata;
import com.tramchester.cloud.SignalToCloudformationReady;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.ClosedStations;
import com.tramchester.graph.PathToStagesMapper;
import com.tramchester.mappers.GenericJourneyResponseMapper;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.mappers.TramJourneyResponseMapper;
import com.tramchester.resources.*;
import com.tramchester.services.DateTimeService;
import com.tramchester.services.SpatialService;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Dependencies {

    public static final String TFGM_UNZIP_DIR = "gtdf-out";
    protected final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);

    public void initialise(TramchesterConfig configuration) throws IOException {
        Path inputPath = configuration.getInputDataPath();
        if (configuration.isPullData()) {
            logger.info("Pulling data");

            FetchDataFromUrl fetcher = new FetchDataFromUrl(inputPath, configuration.getTramDataUrl());
            fetcher.fetchData();
        }
        Path outputPath = configuration.getOutputDataPath();
        if (configuration.isFilterData() || configuration.isPullData()) {
            Path inputDir = inputPath.resolve(TFGM_UNZIP_DIR);
            TransportDataReader reader = new TransportDataReader(inputDir);
            TransportDataWriterFactory writerFactory = new TransportDataWriterFactory(outputPath);

            DataCleanser dataCleanser = new DataCleanser(reader, writerFactory);
            dataCleanser.run(configuration);
            logger.info("Data cleansing finished");
        }
        logger.info("Creating dependencies");
        // caching on by default
        picoContainer.addComponent(TramchesterConfig.class, configuration);
        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(ClosedStations.class);
        picoContainer.addComponent(VersionResource.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(NodeFactory.class);
        picoContainer.addComponent(RelationshipFactory.class);
        picoContainer.addComponent(RouteCalculator.class);
        if (configuration.useGenericMapper()) {
            picoContainer.addComponent(GenericJourneyResponseMapper.class);
        } else {
            picoContainer.addComponent(TramJourneyResponseMapper.class);
        }
        picoContainer.addComponent(RouteCodeToClassMapper.class);

        TransportDataReader dataReader = new TransportDataReader(outputPath);
        TransportDataImporter transportDataImporter = new TransportDataImporter(dataReader);
        picoContainer.addComponent(TransportDataFromFiles.class, transportDataImporter.load());
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(DateTimeService.class);
        picoContainer.addComponent(ConfigFromInstanceUserData.class);
        picoContainer.addComponent(FetchInstanceMetadata.class);
        picoContainer.addComponent(SignalToCloudformationReady.class);
        picoContainer.addComponent(PathToStagesMapper.class);
        picoContainer.addComponent(LocationToLocationJourneyPlanner.class);

        rebuildGraph(configuration);

        logger.info("Prepare to signal cloud formation if running in cloud");
        SignalToCloudformationReady signaller = picoContainer.getComponent(SignalToCloudformationReady.class);
        signaller.send();
    }

    private void rebuildGraph(TramchesterConfig configuration) throws IOException {
        String graphName = configuration.getGraphName();
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();

        if (configuration.isRebuildGraph()) {
            logger.info("Deleting previous graph db for " + graphName);
            try {
                FileUtils.deleteDirectory(new File(graphName));
            } catch (IOException e) {
                logger.error("Error deleting the graph!",e);
                throw e;
            }
            picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseFactory.newEmbeddedDatabase(graphName));
            picoContainer.getComponent(TransportGraphBuilder.class).buildGraph();
            logger.info("Graph rebuild is finished for " + graphName);
        } else {
            logger.info("Not rebuilding graph " + graphName + ". Loading graph");
            picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseFactory.newEmbeddedDatabase(graphName));
        }
        logger.info("graph db ready for " + graphName);
    }

    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

    public void close() {
        GraphDatabaseService graphService = picoContainer.getComponent(GraphDatabaseService.class);
        if (graphService==null) {
                logger.error("Unable to obtain GraphDatabaseService for shutdown");
        } else {
            if (graphService.isAvailable(1)) {
                logger.info("Shutting down graphDB");
                graphService.shutdown();
            }
        }
    }
}
