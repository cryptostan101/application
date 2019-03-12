package com.example;

import com.example.dao.EventDao;
import com.example.domain.Event;
import com.example.domain.EventConverter;
import com.example.domain.EventDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.example.domain.EventDto.State.STARTED;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class App implements CommandLineRunner {
    private static Logger log = LoggerFactory.getLogger(App.class);
    private final ObjectMapper objectMapper;
    private final Connection connection;
    private final EventConverter eventConverter;

    private Map<String, EventDto> startedMap = new ConcurrentHashMap<>();
    private Map<String, EventDto> finishedMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        log.info("Starting application");
        SpringApplication.run(App.class, args);
        log.info("Finishing application");
    }

    @Autowired
    public App(ObjectMapper objectMapper, Connection connection, EventConverter eventConverter) {
        this.objectMapper = objectMapper;
        this.connection = connection;
        this.eventConverter = eventConverter;
    }

    @Override
    public void run(String... args) throws IOException {
        if (args.length != 1 || args[0].isEmpty()) {
            throw new InvalidParameterException("Add log file Path to argument e.g -> src/resources/test.txt");
        }

        String filePath = args[0];

        log.info("Open file {} for processing", filePath);
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            bufferedReader.lines().forEach(this::groupByState);
            processData(startedMap.keySet());

            log.info("Completed processing file", filePath);
        } catch (IOException e) {
            log.error("Err reading the file, exiting...", e);
            throw e;
        }
    }

    /**
     * Inputs from json object and converts it to eventDTO object, then based on state saves to start/finish hashmap
     *
     * @param json
     */
    private void groupByState(String json) {
        try {
            log.info("Converting JSON to EventDTO");
            EventDto eventDTO = Optional.ofNullable(objectMapper.readValue(json, EventDto.class))
                    .orElseThrow(() -> new NullPointerException("Err converting json to EventDTO"));

            log.info("Create STARTED and FINISHED events map");
            if (eventDTO.getState().equals(STARTED)) {
                startedMap.put(eventDTO.getId(), eventDTO);
            } else {
                finishedMap.put(eventDTO.getId(), eventDTO);
            }
        } catch (IOException e) {
            log.error("Err processing json object, skipping...", json);
        }
    }

    /**
     * Takes event ids and finds corresponding start and finish events if found saves resulting event and its duration
     *
     * @param ids
     */
    private void processData(Set<String> ids) {
        try(EventDao eventDao = new EventDao(connection)) {
            for (String id : ids) {
                EventDto startEvent = startedMap.get(id);
                EventDto finishEvent = finishedMap.get(id);
                if (startEvent!= null && finishEvent != null) {
                    log.info("Converting eventDTO to event...");
                    Event event = eventConverter.EventDTOToEvent(startEvent, finishEvent);

                    log.info("Saving {}", event.toString());
                    eventDao.save(event);
                } else {
                    log.error("Log {} ids is missing start or finish event, skipping...", id);
                }
            }
        }
    }
}
