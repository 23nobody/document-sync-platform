package com.acme.docsync.app;

import com.acme.docsync.metadata.FileMetadataRegistry;
import com.acme.docsync.metadata.FingerprintCalculator;
import com.acme.docsync.metadata.FormatProfiler;
import com.acme.docsync.metadata.MetadataRegistry;
import com.acme.docsync.format.FormatPluginRegistry;
import com.acme.docsync.model.IngestionContractValidator;
import com.acme.docsync.storage.FileProvenanceStore;
import com.acme.docsync.storage.FileCanonicalDocumentStore;
import com.acme.docsync.storage.FileIndexPublishStateStore;
import com.acme.docsync.storage.FileTombstoneStore;
import com.acme.docsync.storage.ConsistencyChecker;
import com.acme.docsync.storage.ProvenanceStore;
import com.acme.docsync.storage.CanonicalDocumentStore;
import com.acme.docsync.storage.IndexPublishStateStore;
import com.acme.docsync.storage.TombstoneStore;
import com.acme.docsync.storage.TenantPathPolicy;
import com.acme.docsync.storage.TenantPathResolver;
import com.acme.docsync.sync.EventIngressQueue;
import com.acme.docsync.sync.EventOrderingResolver;
import com.acme.docsync.sync.EventPipeline;
import com.acme.docsync.sync.DriftDetector;
import com.acme.docsync.sync.FileProcessedEventStore;
import com.acme.docsync.sync.FileSourceSnapshotStore;
import com.acme.docsync.sync.ProcessedEventStore;
import com.acme.docsync.sync.ReconciliationMetrics;
import com.acme.docsync.sync.ReconciliationScanner;
import com.acme.docsync.sync.ReconciliationScheduler;
import com.acme.docsync.sync.RuntimeHealthService;
import com.acme.docsync.sync.SourceSnapshotStore;
import com.acme.docsync.sync.WatcherEventMapper;
import com.acme.docsync.transform.CanonicalOutputValidator;
import com.acme.docsync.transform.CanonicalTransformer;
import com.acme.docsync.transform.DeleteCoordinator;
import com.acme.docsync.transform.PluginCanonicalTransformer;
import com.acme.docsync.transform.PublishCoordinator;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Application entry point.
 */
public final class Application {
  private Application() {
  }

  /**
   * Starts the application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    TenantPathPolicy tenantPathPolicy = new TenantPathPolicy();
    IngestionContractValidator ingestionContractValidator =
        new IngestionContractValidator(tenantPathPolicy);
    new TenantPathResolver(Path.of("./data"), tenantPathPolicy);
    EventOrderingResolver eventOrderingResolver = new EventOrderingResolver();
    ProcessedEventStore processedEventStore = new FileProcessedEventStore(
        Path.of("./data", "state", "processed-events.properties"));
    EventIngressQueue ingressQueue = new EventIngressQueue(1024);
    EventPipeline eventPipeline = new EventPipeline(
        ingestionContractValidator,
        eventOrderingResolver,
        processedEventStore,
        ingressQueue);
    new WatcherEventMapper(ingestionContractValidator);
    SourceSnapshotStore snapshotStore = new FileSourceSnapshotStore(
        Path.of("./data", "state", "snapshots"));
    ReconciliationScanner reconciliationScanner = new ReconciliationScanner(
        List.of(),
        snapshotStore,
        new DriftDetector(),
        eventPipeline);
    ReconciliationScheduler reconciliationScheduler = new ReconciliationScheduler(
        reconciliationScanner,
        new ReconciliationMetrics(),
        Duration.ofMinutes(5));
    MetadataRegistry metadataRegistry = new FileMetadataRegistry(
        Path.of("./data", "state", "metadata"));
    FormatProfiler formatProfiler = new FormatProfiler(new FingerprintCalculator());
    FormatPluginRegistry formatPluginRegistry = new FormatPluginRegistry();
    CanonicalOutputValidator canonicalOutputValidator = new CanonicalOutputValidator();
    CanonicalTransformer canonicalTransformer = new PluginCanonicalTransformer(
        formatPluginRegistry, canonicalOutputValidator);
    ProvenanceStore provenanceStore = new FileProvenanceStore(
        Path.of("./data", "state", "provenance"));
    CanonicalDocumentStore canonicalDocumentStore = new FileCanonicalDocumentStore(
        Path.of("./data", "state", "canonical"));
    IndexPublishStateStore indexPublishStateStore = new FileIndexPublishStateStore(
        Path.of("./data", "state", "index"));
    TombstoneStore tombstoneStore = new FileTombstoneStore(
        Path.of("./data", "state", "tombstones"));
    PublishCoordinator publishCoordinator = new PublishCoordinator(
        canonicalDocumentStore, indexPublishStateStore);
    DeleteCoordinator deleteCoordinator = new DeleteCoordinator(
        canonicalDocumentStore, indexPublishStateStore, tombstoneStore);
    RuntimeHealthService runtimeHealthService = new RuntimeHealthService(
        ingressQueue, reconciliationScheduler, new ReconciliationMetrics());
    ConsistencyChecker consistencyChecker = new ConsistencyChecker(
        canonicalDocumentStore, indexPublishStateStore, tombstoneStore);

    if (eventPipeline == null
        || reconciliationScheduler == null
        || metadataRegistry == null
        || formatProfiler == null
        || canonicalTransformer == null
        || provenanceStore == null
        || publishCoordinator == null
        || deleteCoordinator == null
        || runtimeHealthService == null
        || consistencyChecker == null) {
      throw new IllegalStateException("failed to initialize event pipeline");
    }
    System.out.println("Document Sync Platform initialized.");
  }
}
