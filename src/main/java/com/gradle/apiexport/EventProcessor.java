package com.gradle.apiexport;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import com.opencsv.*;


class EventProcessor {

    String buidlId;
    //Map of task id to Task object
    Map<String, Task> taskMap = new HashMap();

    CSVWriter csvWriter;

    private Writer createFileWriter(String fileName) {
        try {
            return new FileWriter(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public EventProcessor(String buildId) {
        this.buidlId = buildId;
        csvWriter = new CSVWriter(createFileWriter("tasks.csv"), '\t');
        /*
     // feed in your array (or convert your data to an array)
     String[] entries = "first#second#third".split("#");
     writer.writeNext(entries);
	 writer.close();*/
    }
    public void process(JsonNode json) {
        String eventType = json.get("type").get("eventType").asText();

        switch (eventType) {
            case "TaskStarted":
                taskStarted(json);
                break;
            case "TaskFinished":
                taskFinished(json);
                break;
        }

    }

    /*
    id: 35
event: BuildEvent
data: {"timestamp":1488495221555,"type":{"majorVersion":1,"minorVersion":2,"eventType":"TaskStarted"},"data":{"id":-2556824238716145285,"path":":compileJava","className":"org.gradle.api.tasks.compile.JavaCompile_Decorated","thread":0,"noActions":false}}
     */
    void taskStarted(JsonNode json) {
        JsonNode data = json.get("data");
        JsonNode id = data.get("id");
        assert id != null;
        String key = id.asText();

        assert taskMap.get(key) == null;

        Task task = new Task();
        task.buildId = this.buidlId;
        task.path = data.get("path").asText();
        task.timer.startTime = Instant.ofEpochMilli(json.get("timestamp").asLong());

        taskMap.put(key, task);
    }

    /*Example:
    id: 38
event: BuildEvent
data: {"timestamp":1488495221566,"type":{"majorVersion":1,"minorVersion":3,"eventType":"TaskFinished"},"data":{"id":-2556824238716145285,"path":":compileJava","outcome":"up_to_date","skipMessage":null,"cacheable":false,"cachingDisabledExplanation":null}}

id: 39
     */
    void taskFinished(JsonNode json) {
        JsonNode id = json.get("data").get("id");
        assert id != null;
        String key = id.asText();

        Task task = taskMap.get(key);
        assert task != null;

        JsonNode timestamp = json.get("timestamp");
        assert timestamp != null;
        task.timer.finishTime = Instant.ofEpochMilli(timestamp.asLong());
        System.out.println("Task: " + task);
        // insert into DB
        taskMap.remove(key);
    }
}