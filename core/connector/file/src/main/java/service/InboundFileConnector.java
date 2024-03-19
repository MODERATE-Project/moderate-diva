package service;

import com.linksfoundation.dq.connectors.interfaces.InboundConnectorInterface;
import com.linksfoundation.dq.connectors.service.ConnectorManagerService;
import com.linksfoundation.dq.model.Sample;
import com.linksfoundation.dq.model.StringArray;
import com.linksfoundation.dq.utils.exceptions.ConnectorFileNotFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

@Service
@Slf4j
public class InboundFileConnector implements InboundConnectorInterface, CommandLineRunner {

    @Value(value = "${IN_FILE_PATH}")
    private String filePath;
    @Value(value = "${DATASET_NAME}")
    private String datasetName;
    @Value(value = "${N_ROWS}")
    private String nRows;

    protected ConnectorManagerService manager;

    public InboundFileConnector(ConnectorManagerService manager) {
        this.manager = manager;
    }

    @Override
    public Flux<Sample> importSamples() {

        List<Sample> samples = new LinkedList<>();
        try (Scanner sc = new Scanner(new File(filePath))){
            String header = sc.nextLine();
            int n = Integer.parseInt(nRows);
            // TODO: 50 only for TESTING
            for (int i = 0; i < n; i++) {
                Sample sample = Sample.newBuilder()
                        .setTs(System.currentTimeMillis())
                        .setState(Sample.States.RAW)
                        .setDataset(datasetName)
                        .putStringData("header", StringArray.newBuilder().addElement(header).build())
                        .putStringData("row", StringArray.newBuilder().addElement(sc.nextLine()).build())
                        .build();

                samples.add(sample);
                sleep(5);
            }

        } catch (FileNotFoundException e) {
            throw new ConnectorFileNotFound();
        }

        return Flux.fromIterable(samples)
                .doOnNext(sample -> this.manager.getSamplesIn().emitNext(sample, Sinks.EmitFailureHandler.FAIL_FAST));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run(String... args)  {
        if (filePath.length() > 0) {
            log.info("InboundFileConnector is starting...");
            importSamples().subscribe();
        }
        else {
            log.info("Input file not specified. Feature disabled.");
        }
    }
}
