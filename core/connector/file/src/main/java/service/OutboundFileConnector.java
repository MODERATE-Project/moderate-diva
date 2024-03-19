package service;

import com.linksfoundation.dq.connectors.interfaces.OutboundConnectorInterface;
import com.linksfoundation.dq.connectors.service.ConnectorManagerService;
import com.linksfoundation.dq.model.BoolArray;
import com.linksfoundation.dq.model.FloatArray;
import com.linksfoundation.dq.model.Sample;
import com.linksfoundation.dq.model.StringArray;
import com.linksfoundation.dq.utils.exceptions.ConnectorFileNotFound;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class OutboundFileConnector implements OutboundConnectorInterface, CommandLineRunner {

    @Value(value = "${OUT_FILE_PATH}")
    private String filePath;

    protected ConnectorManagerService manager;
    public OutboundFileConnector(ConnectorManagerService manager) {
        this.manager = manager;
    }

    protected Flux<Sample> sampleFlux() {
        return this.manager.getSamplesOut().asFlux();
    }

    private Flux<Sample> receive()  {
        return sampleFlux()
                .doOnNext(row -> this.exportSamples(row));
    }
    @Override
    public void exportSamples(Sample sample) {

        File f = new File(filePath);
        String header = this.getHeader(sample);
        BufferedWriter out = null;
        if (!f.exists()) {
            try {
                out = new BufferedWriter(new FileWriter(filePath));
                out.write(header);
                out.newLine();
                out.flush();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> rows = this.sampleToRow(sample, header);
        try {
            out = new BufferedWriter(new FileWriter(filePath, true));

            for (String row : rows) {
                out.append(row);
                out.newLine();
            }

            out.flush();
            out.close();
        } catch (IOException e) {
            throw new ConnectorFileNotFound();
        }
    }

    private String getHeader(Sample sample) {
        List<String> fields = new LinkedList<>();
        if (sample.getFloatDataMap() != null) {
            fields.addAll(sample.getFloatDataMap().keySet());
        }
        if (sample.getStringDataMap() != null) {
            fields.addAll(sample.getStringDataMap().keySet());
        }
        if (sample.getBoolDataMap() != null) {
            fields.addAll(sample.getBoolDataMap().keySet());
        }

        return String.join(",", fields);
    }

    private List<String> sampleToRow(Sample sample, String header) {

        List<String> rows = new LinkedList<>();
        int nRows;
        Map<String, FloatArray> floats = sample.getFloatDataMap();
        Map<String, StringArray> strings = sample.getStringDataMap();
        Map<String, BoolArray> booleans = sample.getBoolDataMap();

        if (floats.keySet().size() > 0) {
            nRows = floats
                    .get(floats.keySet().toArray()[0])
                    .getElementList()
                    .size();
        } else if (strings.keySet().size() > 0) {
            nRows = strings
                    .get(strings.keySet().toArray()[0])
                    .getElementList()
                    .size();
        } else if (booleans.keySet().size() > 0) {
            nRows = booleans
                    .get(booleans.keySet().toArray()[0])
                    .getElementList()
                    .size();
        } else  {
            nRows = 0;
        }

        for (int i = 0; i < nRows; i++) {

            List<String> fields = new LinkedList<>();

            for (String column : header.split(",")) {
                if (floats.containsKey(column)) {
                    fields.add(String.valueOf(floats.get(column).getElement(i)));
                } else if (strings.containsKey(column)) {
                    fields.add(strings.get(column).getElement(i));
                } else if (booleans.containsKey(column)){
                    fields.add(String.valueOf(booleans.get(column).getElement(i)));
                }
            }

            rows.add(String.join(",", fields));
        }

       return rows;
    }
    
    @Override
    public void run(String... args)  {
        if (filePath.length() > 0) {
            log.info("OutboundFileConnector is starting...");
            this.receive().subscribe();
        }
        else {
            log.info("Output file not specified. Feature disabled.");
        }
    }
}
